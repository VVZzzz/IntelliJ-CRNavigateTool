package com.wrh.dev.crnavigatetool;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.Side;
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsCollectorImpl;
import com.intellij.internal.statistic.eventLog.events.EventFields;
import com.intellij.internal.statistic.eventLog.events.EventPair;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.openapi.editor.EditorKind.DIFF;

public class GotoDeclareCustomAction extends GotoDeclarationAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("enter Custom GotoDeclareCustomAction");
        Editor editor = super.getEditor(e.getDataContext(),e.getProject(),false);
        EditorKind kind = editor.getEditorKind();
        System.out.println(kind);

        if (kind == DIFF) {
            handleDiffRedirect(e,editor,e.getDataContext());
        } else {
            // 默认跳转逻辑
            super.actionPerformed(e);
        }
    }

    //2.重定向跳转到 Diff 视图
    private void handleDiffRedirect(@NotNull AnActionEvent e, Editor editor, DataContext dataContext) {
        Project project = editor.getProject();
        if (project == null) {
            System.out.println("getProject empty");
            return;
        }

        // 2.1. 获取光标指定目标 PsiElement
        PsiElement targetElement = getTargetElement(editor, dataContext);
        if (targetElement == null) {
            System.out.println("targetElement empty");
            super.actionPerformed(e);
            return;
        }

        // 2.2. 获取 PsiElement 对应的目标文件(文件系统中的实际文件)
        VirtualFile targetFile = getContainingVirtualFile(targetElement);
        if (targetFile == null) {
            System.out.println("targetFile empty");
            super.actionPerformed(e);
            return;
        }



        // 2.3. 获取当前 Diff 上下文
        ContentDiffRequest currentRequest = getCurrentDiffRequest(dataContext);
        if (currentRequest == null) {
            System.out.println("currentRequest empty");
            super.actionPerformed(e);
            return;
        }

        // 2.4. 在 Diff 视图中打开目标文件
        navigateInDiffView(project, targetFile, targetElement, currentRequest);
    }

    private PsiElement getTargetElement(Editor editor, DataContext context) {
        Navigatable navigatable = context.getData(CommonDataKeys.NAVIGATABLE);
        // 查看是否是 PsiElement 的一个实例
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

        // List<DiffContent> 会有两个内容, left: before(历史版本version)文件 right:current 当前实际文件
        // 随场景变化
        List<DiffContent> contents = currentRequest.getContents();
        if (contents.size() < 2) {
            System.out.println("contents.size() < 2");
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

        System.out.println("start show diff");
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






    @Override
    public void update(@NotNull AnActionEvent e) {
        System.out.println("CustomAction update called");
        super.update(e);
    }
}
