from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator

Probability = Annotated[float, Field(ge=0, le=1, allow_inf_nan=False)]
RawScore = Annotated[float, Field(ge=0, le=4, allow_inf_nan=False)]


class Output(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Evidence(Output):
    sourcePath: str = Field(pattern=r"^/")
    start: int = Field(ge=0)
    end: int = Field(ge=0)
    text: str

    @model_validator(mode="after")
    def valid_span(self):
        if self.end - self.start != len(self.text):
            raise ValueError("invalid Unicode span")
        return self


class Extraction(Output):
    candidateId: str
    text: str
    evidence: list[Evidence]
    probability: Probability | None
    importance: RawScore | None


class Usage(Output):
    inputTokens: Annotated[int, Field(ge=0)] | None
    outputTokens: Annotated[int, Field(ge=0)] | None


class AnalysisResponse(Output):
    jobId: str
    status: Literal["complete", "no_evidence", "needs_review"]
    requirements: list[Extraction]
    benefits: list[Extraction]
    coreRequirements: list[Extraction]
    model: str
    pipelineVersion: str
    policyVersion: str
    requestId: str
    usage: Usage


class Dimension(Output):
    name: Literal["skills", "relevant_experience", "experience", "education"]
    status: Literal["scored", "unknown", "not_applicable"]
    rawScore: RawScore | None
    confidence: Probability | None
    probabilities: dict[str, Probability] | None
    evidence: list[Evidence]


class MatchItem(Output):
    jobId: str
    status: Literal["scored", "insufficient_evidence", "failed"]
    score: Annotated[float, Field(ge=0, le=10, allow_inf_nan=False)] | None
    dimensions: list[Dimension]


class MatchResponse(Output):
    items: list[MatchItem]
    complete: bool
    model: str
    policyVersion: str
    requestId: str
    usage: Usage
