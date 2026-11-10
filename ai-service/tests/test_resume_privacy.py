"""These tests deliberately do not bypass the production privacy guard."""
from dataclasses import replace

import httpx
import pytest

from app.config import Settings
from app.jev import Jev, ServiceError
from app.main import MatchRequest, create_app
from app.pipeline import Pipeline

CFG = Settings(zdr=True, api_key="synthetic-provider", service_token="synthetic-internal",
               model="typesafe/jev-1.14.0")
AUTH = {"Authorization": "Bearer synthetic-internal"}


@pytest.mark.parametrize("resume", ["", "Inline Synthetic Person passport SYNTHETIC-ID",
                                   "**姓名**: 合成姓名\n地址 Synthetic street\n+1 202 555 0100",
                                   "合成😀" * 3000])
@pytest.mark.parametrize("jobs", [[], [{"jobId": "1", "detailData": {"jobTags": ["Java"]}}]])
@pytest.mark.parametrize("zdr", [False, True])
async def test_direct_pipeline_rejects_before_any_provider_or_budget_use(tmp_path, resume, jobs, zdr):
    path = tmp_path / "budget.sqlite3"
    def forbidden(request):
        pytest.fail("Resume reached provider transport")
    jev = Jev(replace(CFG, zdr=zdr, budget_path=str(path)), httpx.MockTransport(forbidden))
    try:
        with pytest.raises(ServiceError) as caught:
            await Pipeline(jev).match(MatchRequest(resume=resume, jobs=jobs))
        assert (caught.value.status, caught.value.code) == (503, "resume_matching_disabled")
        assert not path.exists()
    finally:
        await jev.close()


@pytest.mark.parametrize("suffix", ["", "/"])
@pytest.mark.parametrize("zdr", [False, True])
async def test_http_rejects_before_consuming_body_and_never_echoes_it(tmp_path, suffix, zdr, caplog):
    path = tmp_path / "budget.sqlite3"
    def forbidden(request):
        pytest.fail("Resume reached provider transport")
    app = create_app(replace(CFG, zdr=zdr, budget_path=str(path), quality_approved=True), httpx.MockTransport(forbidden))
    async def unread_body():
        pytest.fail("Disabled endpoint consumed a resume body")
        yield b"synthetic"
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://internal") as client:
        response = await client.post("/internal/v1/match-batch" + suffix, headers=AUTH, content=unread_body())
        assert response.status_code == 503
        assert response.json()["error"]["code"] == "resume_matching_disabled"
        assert response.json()["requestId"]
        assert "synthetic" not in response.text
        assert not path.exists()
        ready = (await client.get("/health/ready")).json()
        assert ready["resumeMatching"]["enabled"] is False
        assert (await client.get("/health/live")).status_code == 200
        assert (await client.post("/internal/v1/match-batch", json={})).status_code == 401
    assert "Synthetic Person" not in caplog.text
    await app.state.jev.close()
