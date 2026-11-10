package com.david.hlp.crawler.ai.service;

/** Fail closed until matching has an independently verified data-minimization contract. */
final class ResumePrivacy {
    private ResumePrivacy() { }

    static void requireSafeMatching() {
        // ZDR controls retention, not what leaves this service. No configuration may bypass this guard.
        throw AIExtractionOfJobRequirements.error(503, "AI_RESUME_MATCHING_DISABLED");
    }
}
