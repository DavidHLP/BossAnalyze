import asyncio
import json
import logging
import secrets
from contextlib import asynccontextmanager
from uuid import uuid4

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, ConfigDict, Field, StrictStr

from .config import Settings
from .schemas import AnalysisResponse, MatchResponse
from .jev import Jev, ServiceError, unique_object
from .pipeline import Pipeline, WEIGHTS
from .privacy import require_safe_matching


class Job(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    jobId: StrictStr = Field(min_length=1, max_length=128)
    detailData: dict


class AnalysisRequest(Job):
    timeoutMs: int = Field(default=30000, gt=0)


class MatchRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    resume: StrictStr = Field(max_length=12000)
    jobs: list[Job]
    timeoutMs: int = Field(default=30000, gt=0)


def create_app(settings=None, transport=None):
    cfg = settings or Settings.from_env()
    jev = Jev(cfg, transport)
    pipeline = Pipeline(jev)
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)

    @asynccontextmanager
    async def lifespan(app):
        yield
        await jev.close()

    app = FastAPI(lifespan=lifespan, docs_url=None, redoc_url=None, openapi_url=None)
    app.state.jev = jev

    def error(code, status, request_id):
        return JSONResponse(dict(error=dict(code=code, message="AI service request could not be completed"),
                                 requestId=request_id), status_code=status)

    @app.middleware("http")
    async def boundary(request: Request, call_next):
        request.state.request_id = str(uuid4())
        rid = request.state.request_id
        if request.url.path.startswith("/internal/"):
            expected = "Bearer " + cfg.service_token
            if not cfg.service_token or not secrets.compare_digest(request.headers.get("authorization", "").encode(), expected.encode()):
                return error("internal_unauthorized", 401, rid)
            if not cfg.ready:
                return error("not_configured", 503, rid)
            if request.url.path.rstrip("/") == "/internal/v1/match-batch":
                try:
                    require_safe_matching()
                except ServiceError as exc:
                    return error(exc.code, exc.status, rid)
            body = bytearray()
            try:
                async with asyncio.timeout(cfg.deadline_ms / 1000):
                    async for chunk in request.stream():
                        body.extend(chunk)
                        if len(body) > cfg.max_input_bytes:
                            return error("input_limit", 422, rid)
            except TimeoutError:
                return error("deadline_exhausted", 504, rid)
            try:
                json.loads(body, object_pairs_hook=unique_object,
                           parse_constant=lambda _: (_ for _ in ()).throw(ValueError()))
            except (ValueError, UnicodeDecodeError, RecursionError):
                return error("invalid_input", 400, rid)
            request._body = bytes(body)
        try:
            # Match owns its inference deadline; allow bounded cancellation/response cleanup afterward.
            cleanup = 1 if request.url.path == "/internal/v1/match-batch" else 0
            async with asyncio.timeout(cfg.deadline_ms / 1000 + cleanup):
                response = await call_next(request)
            response.headers["X-Request-ID"] = rid
            return response
        except (TimeoutError, asyncio.TimeoutError):
            return error("deadline_exhausted", 504, rid)
        except Exception:
            return error("internal_error", 500, rid)

    @app.exception_handler(ServiceError)
    async def service_error(request, exc):
        return error(exc.code, exc.status, request.state.request_id)

    @app.exception_handler(RequestValidationError)
    async def validation_error(request, exc):
        return error("invalid_input", 422, request.state.request_id)

    @app.get("/health/live")
    async def live():
        return {"status": "alive"}

    @app.get("/health/ready")
    async def ready(request: Request):
        if not cfg.ready:
            return error("not_configured", 503, request.state.request_id)
        return {"status": "ready", "model": cfg.model, "policyVersion": "jev-policy-1",
                "resumeMatching": {"enabled": False, "reason": "resume_matching_disabled"},
                "qualityGate": {"calibrated": cfg.quality_approved, "automaticWritebackAllowed": cfg.quality_approved,
                                "acceptThreshold": cfg.accept, "rejectThreshold": cfg.reject,
                                "matchWeights": WEIGHTS, "minimumCandidateRecall": 0.98, "minimumAcceptedPrecision": 0.95,
                                "requiredEvidenceConsistency": 1.0}}

    @app.post("/internal/v1/job-analysis", response_model=AnalysisResponse)
    async def analyze(req: AnalysisRequest, request: Request):
        async with asyncio.timeout(min(req.timeoutMs, cfg.deadline_ms) / 1000):
            result = await pipeline.analyze(req)
        return dict(result, requestId=request.state.request_id)

    @app.post("/internal/v1/match-batch", response_model=MatchResponse)
    async def match(req: MatchRequest, request: Request):
        ids = [j.jobId for j in req.jobs]
        if len(ids) > cfg.max_jobs or len(ids) != len(set(ids)):
            raise ServiceError("invalid_job_ids_or_limit", 422)
        result = await pipeline.match(req)
        return dict(result, requestId=request.state.request_id)

    return app


app = create_app()
