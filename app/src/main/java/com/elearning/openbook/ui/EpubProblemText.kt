package com.elearning.openbook.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elearning.openbook.R
import com.elearning.openbook.model.EpubProblem

/** One localized line per problem, so four different faults never share one vague message. */
@Composable
fun epubProblemText(problem: EpubProblem): String = stringResource(
    when (problem) {
        EpubProblem.NOT_ZIP -> R.string.epub_error_not_zip
        EpubProblem.NO_CONTAINER -> R.string.epub_error_no_container
        EpubProblem.NO_OPF -> R.string.epub_error_no_opf
        EpubProblem.EMPTY_SPINE -> R.string.epub_error_empty_spine
        EpubProblem.DRM -> R.string.epub_error_drm
        EpubProblem.GENERIC -> R.string.epub_error_generic
    },
)
