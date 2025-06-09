import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.Side;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;

import java.util.List;

public class DiffAwareGotoHandler extends EditorActionHandler {
    private final EditorActionHandler originalHandler;

    public DiffAwareGotoHandler(EditorActionHandler originalHandler) {
        this.originalHandler = originalHandler;
    }

    @Override
    protected void doExecute(@NotNull Editor editor, Caret caret, DataContext dataContext) {
        if (isInDiffContext(editor)) {
            handleDiffRedirect(editor, dataContext);
        } else {
            originalHandler.execute(editor, caret, dataContext);
        }
    }

    // 后续方法将在这里实现

    //1.是否是在Diff窗口中
    private boolean isInDiffContext(Editor editor) {
        Project project = editor.getProject();
        if (project == null) return false;

        ToolWindow diffWindow = ToolWindowManager.getInstance(project).getToolWindow("Diff");
        return diffWindow != null && diffWindow.isVisible();
    }

    //2.重定向跳转到 Diff 视图
    private void handleDiffRedirect(Editor editor, DataContext dataContext) {
        Project project = editor.getProject();
        if (project == null) return;

        // 2.1. 获取目标元素
        PsiElement targetElement = getTargetElement(editor, dataContext);
        if (targetElement == null) {
            originalHandler.execute(editor, null, dataContext);
            return;
        }

        // 2.2. 获取目标文件
        VirtualFile targetFile = getContainingVirtualFile(targetElement);
        if (targetFile == null) return;



        // 2.3. 获取当前 Diff 上下文
        ContentDiffRequest currentRequest = getCurrentDiffRequest(dataContext);
        if (currentRequest == null) {
            originalHandler.execute(editor, null, dataContext);
            return;
        }

        // 2.4. 在 Diff 视图中打开目标文件
        navigateInDiffView(project, targetFile, targetElement, currentRequest);
    }

    private PsiElement getTargetElement(Editor editor, DataContext context) {
        Navigatable navigatable = context.getData(CommonDataKeys.NAVIGATABLE);
        if (navigatable instanceof PsiElement) {
            return (PsiElement) navigatable;
        }
        return null;
    }

    private VirtualFile getContainingVirtualFile(PsiElement element) {
        PsiFile file = element.getContainingFile();
        return file != null ? file.getVirtualFile() : null;
    }

    //private DiffContent getCurrentDiffContent() {
    //    // 通过 DiffManager 获取当前 Diff 内容
    //    DiffRequestPanel diffPanel = DiffManager.getInstance()  getDiffRequestPanel();
    //    return diffPanel != null ? diffPanel.getContent() : null;
    //}

    // 使用DiffDataKeys获取当前Diff请求
    @Nullable
    private ContentDiffRequest getCurrentDiffRequest(DataContext dataContext) {
        return (ContentDiffRequest) dataContext.getData(DiffDataKeys.DIFF_REQUEST);
    }



    private void navigateInDiffView(Project project,
                                    VirtualFile targetFile,
                                    PsiElement targetElement,
                                    ContentDiffRequest currentRequest) {

        List<DiffContent> contents = currentRequest.getContents();
        if (contents.size() < 2) {
            return; // 不是标准的两边对比
        }

        // 创建目标文件的Diff请求
        SimpleDiffRequest request = new SimpleDiffRequest(
                targetFile.getName(),
                createTargetDiffContent(project, targetFile, contents.get(0)),
                createTargetDiffContent(project, targetFile, contents.get(1)),
                currentRequest.getContentTitles().get(0),
                currentRequest.getContentTitles().get(1)
        );

        // 设置导航位置
        int lineNumber = getLineNumber(targetElement);
        if (lineNumber >= 0) {
            request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, Pair.create(Side.RIGHT, lineNumber));
        }

        // 在现有Diff窗口打开
        DiffManager.getInstance().showDiff(project, request /*, new DiffDialogHints(
                ToolWindowAnchor.RIGHT,
                "DiffRedirect"
        )*/);
    }

    private int getLineNumber(PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null) return -1;
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) return -1;

        var document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) return -1;

        int offset = element.getTextOffset();
        return document.getLineNumber(offset);
    }

    private DiffContent createTargetDiffContent(Project project,
                                                VirtualFile targetFile,
                                                DiffContent originalContent) {
        // 如果当前内容已经是目标文件，则复用
        //VirtualFile originalFile = originalContent.getFile();
        //if (originalFile != null && Objects.equals(originalFile.getPath(), targetFile.getPath())) {
        //    return originalContent;
        //}

        // 否则创建新的内容
        return DiffContentFactory.getInstance().create(project, targetFile);
    }


}
