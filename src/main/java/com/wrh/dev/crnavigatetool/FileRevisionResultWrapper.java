package com.wrh.dev.crnavigatetool;

import com.intellij.openapi.vcs.history.VcsFileRevision;

enum FileRevisionNoFoundReason {
    Unknown,
    NotExistBeforeTargetRevision,
    NoChangeAfterTargetRevision,
}
public class FileRevisionResultWrapper {
    private final VcsFileRevision vcsFileRevision;
    private final FileRevisionNoFoundReason noFoundReason;
    public FileRevisionResultWrapper(VcsFileRevision vcsFileRevision, FileRevisionNoFoundReason noFoundReason) {
        this.vcsFileRevision = vcsFileRevision;
        this.noFoundReason = noFoundReason;
    }

    public VcsFileRevision getVcsFileRevision() {
        return vcsFileRevision;
    }

    public FileRevisionNoFoundReason getNoFoundReason() {
        return noFoundReason;
    }
}
