// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.hierarchy;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class CallHierarchyBrowserBase extends HierarchyBrowserBaseEx {
  public static final String CALLEE_TYPE = "Callees of {0}";
  public static final String CALLER_TYPE = "Callers of {0}";

  public CallHierarchyBrowserBase(@NotNull Project project, @NotNull PsiElement method) {
    super(project, method);
  }

  @Override
  @Nullable
  protected JPanel createLegendPanel() {
    return null;
  }

  @Override
  protected void prependActions(@NotNull DefaultActionGroup actionGroup) {
    actionGroup.add(new ChangeViewTypeActionBase(IdeBundle.message("action.caller.methods.hierarchy"),
                                                 IdeBundle.message("action.caller.methods.hierarchy"),
                                                 AllIcons.Hierarchy.Supertypes, getCallerType()));
    actionGroup.add(new ChangeViewTypeActionBase(IdeBundle.message("action.callee.methods.hierarchy"),
                                                 IdeBundle.message("action.callee.methods.hierarchy"),
                                                 AllIcons.Hierarchy.Subtypes, getCalleeType()));
    actionGroup.add(new AlphaSortAction());
    actionGroup.add(new ChangeScopeAction());
  }

  @Override
  @NotNull
  protected String getActionPlace() {
    return ActionPlaces.CALL_HIERARCHY_VIEW_TOOLBAR;
  }

  @Override
  @NotNull
  protected String getPrevOccurenceActionNameImpl() {
    return IdeBundle.message("hierarchy.call.prev.occurence.name");
  }

  @Override
  @NotNull
  protected String getNextOccurenceActionNameImpl() {
    return IdeBundle.message("hierarchy.call.next.occurence.name");
  }

  @Override
  protected @NotNull Map<String, Supplier<String>> getPresentableNameMap() {
    HashMap<String, Supplier<String>> map = new HashMap<>();
    map.put(CALLER_TYPE, CallHierarchyBrowserBase::getCallerType);
    map.put(CALLEE_TYPE, CallHierarchyBrowserBase::getCalleeType);
    return map;
  }

  private final class ChangeViewTypeActionBase extends ToggleAction {
    private final String myTypeName;

    private ChangeViewTypeActionBase(@NlsActions.ActionText String shortDescription, @NlsActions.ActionDescription String longDescription, Icon icon, String typeName) {
      super(shortDescription, longDescription, icon);
      myTypeName = typeName;
    }

    @Override
    public final boolean isSelected(@NotNull AnActionEvent event) {
      return myTypeName.equals(getCurrentViewType());
    }

    @Override
    public final void setSelected(@NotNull AnActionEvent event, boolean flag) {
      if (flag) {
        // invokeLater is called to update state of button before long tree building operation
        ApplicationManager.getApplication().invokeLater(() -> changeView(myTypeName));
      }
    }

    @Override
    public final void update(@NotNull AnActionEvent event) {
      super.update(event);
      setEnabled(isValidBase());
    }
  }

  protected static class BaseOnThisMethodAction extends BaseOnThisElementAction {
    public BaseOnThisMethodAction() {
      super(IdeBundle.messagePointer("action.base.on.this.method"), CallHierarchyBrowserBase.class, LanguageCallHierarchy.INSTANCE);
    }
  }

  public static @NotNull String getCalleeType() {
    //noinspection UnresolvedPropertyKey
    return IdeBundle.message("title.hierarchy.callees.of");
  }

  public static @NotNull String getCallerType() {
    //noinspection UnresolvedPropertyKey
    return IdeBundle.message("title.hierarchy.callers.of");
  }
}