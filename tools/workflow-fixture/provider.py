"""Fixture-only app: real HTTP/Jev pipeline, synthetic official-provider transport."""
import asyncio
import json
import os
from pathlib import Path
import socket

import httpx
import uvicorn
from app.config import Settings
from app.main import create_app

out = Path(os.environ['WORKFLOW_OUTPUT'])
calls = 0

async def provider(request):
    global calls
    assert str(request.url) == 'https://api.commandcode.ai/provider/v1/systemone'
    assert request.headers['x-cmd-zdr'] == '1'
    assert request.headers['authorization'] == 'Bearer synthetic-provider'
    calls += 1
    (out / 'provider-count.json').write_text(json.dumps({'calls': calls}))
    if (out / 'fail-provider').exists():
        return httpx.Response(503, json={'error': 'synthetic_upstream_failure'})
    payload = json.loads(request.content)
    if (out / 'slow-match').exists() and '"jobId": "2"' in payload['state']:
        try:
            await asyncio.Event().wait()
        finally:
            (out / 'slow-match-cancelled').touch()
    with (out / 'provider-requests.jsonl').open('a') as log:
        log.write(json.dumps(payload, ensure_ascii=False) + '\n')
    value = 2.5 if '"jobId": "2"' in payload['state'] else 3.5
    answers = {}
    for key, question in payload['questions'].items():
        if question['type'] == 'noul':
            answers[key] = dict(type='noul', noul=0.99)
        elif question['type'] == 'choice':
            answers[key] = dict(type='choice', choice='scored', confidence=1,
                                probabilities={k: float(k == 'scored') for k in question['criteria']})
        else:
            lo = int(value)
            answers[key] = dict(type='score', score=value, confidence=0.95,
                                legend={str(i): v for i, v in enumerate(question['criteria'])},
                                probabilities={str(i): 0.5 if i in (lo, lo + 1) else 0 for i in range(5)})
    return httpx.Response(200, json=dict(model=payload['model'], answers=answers,
                                        usage=dict(input_tokens=100, output_tokens=30)))

(out / 'provider-count.json').write_text('{"calls":0}')
app = create_app(Settings(zdr=True, quality_approved=True, api_key='synthetic-provider',
                          service_token='synthetic-internal', model='typesafe/jev-1.14.0',
                          budget_path=str(out / 'budget.sqlite3')), httpx.MockTransport(provider))
sock = socket.socket()
sock.bind(('127.0.0.1', 0))
sock.listen()
(out / 'python-port.txt').write_text(str(sock.getsockname()[1]))
uvicorn.Server(uvicorn.Config(app, log_level='critical', access_log=False)).run(sockets=[sock])
