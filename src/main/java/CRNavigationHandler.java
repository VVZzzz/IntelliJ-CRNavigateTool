import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.diff.util.DiffUserDataKeysEx;
import git4idea.GitRevisionNumber;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;

// 拦截 IDE 跳转动作
public class CRNavigationHandler extends EditorActionHandler {
    private final EditorActionHandler originalHandler;

    public DiffNavigationHandler(EditorActionHandler originalHandler) {
        this.originalHandler = originalHandler;
    }

    @Override
    public void doExecute(@NotNull Editor editor, Caret caret, DataContext dataContext) {
        if (isInGitDiffView(editor)) {
            navigateInDiffView(editor, dataContext);
        } else {
            originalHandler.execute(editor, caret, dataContext);
        }
    }



    // 2.判断当前是否在 Git Diff 视图中：
    private boolean isInGitDiffView(Editor editor) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        // 关键点：检查文件是否来自 Git 的虚拟文件系统
        return file != null && file.getFileSystem().getProtocol().equals("git");
    }

    // 3. 解析跳转目标的位置
    private void navigateInDiffView(Editor editor, DataContext dataContext) {
        PsiElement targetElement = GotoDeclarationAction.findTargetElement(
                editor.getProject(), editor, editor.getCaretModel().getOffset());

        if (targetElement != null) {
            VirtualFile targetFile = targetElement.getContainingFile().getVirtualFile();
            int offset = targetElement.getTextOffset();

            // 获取当前 Diff 的版本范围
            RevisionRange revisionRange = getCurrentDiffRevisionRange(editor);
            if (revisionRange != null) {
                openInDiffViewer(targetFile, offset, revisionRange);
            }
        }
    }

    // 4. 获取当前 Diff 的版本范围
    private RevisionRange getCurrentDiffRevisionRange(Editor editor) {
        FileEditor fileEditor = FileEditorManager.getInstance(editor.getProject()).getSelectedEditor();
        if (fileEditor instanceof TextEditor) {
            DiffRequest diffRequest = ((TextEditor) fileEditor).getUserData(DiffUserDataKeysEx.DIFF_REQUEST);
            if (diffRequest instanceof GitDiffRequest) {
                return ((GitDiffRequest) diffRequest).getRevisionRange();
            }
        }
        return null;
    }

    // 5. 打开 Diff 窗口并定位到目标位置
    private void openInDiffViewer(VirtualFile targetFile, int offset, RevisionRange revisionRange) {
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        GitRevisionNumber targetRevision = (GitRevisionNumber) revisionRange.getTargetRevision();

        // 创建 Diff 请求
        SimpleDiffRequest request = new SimpleDiffRequest(
                "Code Review Navigation",
                createContent(revisionRange.getSourceRevision(), targetFile),
                createContent(targetRevision, targetFile),
                "Base Version",
                "Target Branch"
        );

        // 在 Diff 窗口中打开并定位
        DiffManager.getInstance().showDiff(project, request, new DiffDialogHints()).onProcessed(diff -> {
            Editor diffEditor = diff.getEditor().getEditor();
            int line = diffEditor.getDocument().getLineNumber(offset);
            diffEditor.getScrollingModel().scrollToLine(line, ScrollType.CENTER);
        });
    }

    private Content createContent(Revision revision, VirtualFile file) {
        return new FileContentImpl(revision, file);
    }


}
