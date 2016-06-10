package de.plushnikov.intellij.plugin.processor.handler.singular;

import com.intellij.openapi.project.Project;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import de.plushnikov.intellij.plugin.processor.field.AccessorsInfo;
import de.plushnikov.intellij.plugin.psi.LombokLightFieldBuilder;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
import de.plushnikov.intellij.plugin.util.PsiTypeUtil;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.List;

class SingularMapHandler extends AbstractSingularHandler {

  private static final String KEY = "Key";
  private static final String VALUE = "Value";
  private static final String LOMBOK_KEY = "$key";
  private static final String LOMBOK_VALUE = "$value";

  SingularMapHandler(boolean shouldGenerateFullBodyBlock) {
    super(shouldGenerateFullBodyBlock);
  }

  @Override
  public void appendBuildCall(@NotNull StringBuilder buildMethodParameters, @NotNull String fieldName) {
    final String keyName = fieldName + LOMBOK_KEY;
    final String valueName = fieldName + LOMBOK_VALUE;
    buildMethodParameters.append("new java.util.HashMap() {{\n").
        append("int _count = null == ").append(keyName).append(" ? 0 : ").append(keyName).append(".size();\n").
        append("for(int _i=0; _i<_count; _i++){\n").
        append(" put(").append(keyName).append(".get(_i), ").append(valueName).append(".get(_i));\n").
        append("}\n").append("}}");
  }

  public void addBuilderField(@NotNull List<PsiField> fields, @NotNull PsiVariable psiVariable, @NotNull PsiClass innerClass, @NotNull AccessorsInfo accessorsInfo) {
    final String fieldName = accessorsInfo.removePrefix(psiVariable.getName());

    final Project project = psiVariable.getProject();
    final PsiManager psiManager = psiVariable.getManager();

    final PsiType psiFieldType = psiVariable.getType();

    final PsiType keyType = PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 0);
    final PsiType valueType = PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 1);

    final PsiType builderFieldKeyType = getBuilderFieldType(keyType, project);
    fields.add(new LombokLightFieldBuilder(psiManager, fieldName + LOMBOK_KEY, builderFieldKeyType)
        .withModifier(PsiModifier.PRIVATE)
        .withNavigationElement(psiVariable)
        .withContainingClass(innerClass));

    final PsiType builderFieldValueType = getBuilderFieldType(valueType, project);
    fields.add(new LombokLightFieldBuilder(psiManager, fieldName + LOMBOK_VALUE, builderFieldValueType)
        .withModifier(PsiModifier.PRIVATE)
        .withNavigationElement(psiVariable)
        .withContainingClass(innerClass));
  }

  @NotNull
  protected PsiType getBuilderFieldType(@NotNull PsiType psiType, @NotNull Project project) {
    final PsiManager psiManager = PsiManager.getInstance(project);
    return PsiTypeUtil.createCollectionType(psiManager, CommonClassNames.JAVA_UTIL_ARRAY_LIST, psiType);
  }

  protected void addOneMethodParameter(@NotNull LombokLightMethodBuilder methodBuilder, @NotNull PsiType psiFieldType, @NotNull String singularName) {
    final PsiManager psiManager = methodBuilder.getManager();
    final PsiType keyType = PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 0);
    final PsiType valueType = PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 1);

    methodBuilder.withParameter(singularName + KEY, keyType);
    methodBuilder.withParameter(singularName + VALUE, valueType);
  }

  protected void addAllMethodParameter(@NotNull LombokLightMethodBuilder methodBuilder, @NotNull PsiType psiFieldType, @NotNull String singularName) {
    final PsiManager psiManager = methodBuilder.getManager();
    final PsiType keyType = PsiTypeUtil.extractAllElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 0);
    final PsiType valueType = PsiTypeUtil.extractAllElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 1);

    final PsiType collectionType = PsiTypeUtil.createCollectionType(psiManager, CommonClassNames.JAVA_UTIL_MAP, keyType, valueType);

    methodBuilder.withParameter(singularName, collectionType);
  }

  protected String getClearMethodBody(String psiFieldName, boolean fluentBuilder) {
    final String codeBlockTemplate = "if (this.{0}" + LOMBOK_KEY + " != null) '{'\n this.{0}" + LOMBOK_KEY + ".clear();\n " +
        " this.{0}" + LOMBOK_VALUE + ".clear(); '}'\n {1}";

    return MessageFormat.format(codeBlockTemplate, psiFieldName, fluentBuilder ? "\nreturn this;" : "");
  }

  protected String getOneMethodBody(@NotNull String singularName, @NotNull String psiFieldName, @NotNull PsiType psiFieldType, @NotNull PsiManager psiManager, boolean fluentBuilder) {
    final String codeBlockTemplate = "if (this.{0}" + LOMBOK_KEY + " == null) '{' \n" +
        "this.{0}" + LOMBOK_KEY + " = new java.util.ArrayList<{3}>(); \n" +
        "this.{0}" + LOMBOK_VALUE + " = new java.util.ArrayList<{4}>(); \n" +
        "'}' \n" +
        "this.{0}" + LOMBOK_KEY + ".add({1}" + KEY + ");\n" +
        "this.{0}" + LOMBOK_VALUE + ".add({1}" + VALUE + ");" +
        "{2}";

    final PsiType keyType = PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 0);
    final PsiType valueType = PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 1);

    return MessageFormat.format(codeBlockTemplate, psiFieldName, singularName, fluentBuilder ? "\nreturn this;" : "",
        keyType.getCanonicalText(false), valueType.getCanonicalText(false));
  }

  protected String getAllMethodBody(@NotNull String singularName, @NotNull PsiType psiFieldType, @NotNull PsiManager psiManager, boolean fluentBuilder) {
    final String codeBlockTemplate = "if (this.{0}" + LOMBOK_KEY + " == null) '{' \n" +
        "this.{0}" + LOMBOK_KEY + " = new java.util.ArrayList<{2}>(); \n" +
        "this.{0}" + LOMBOK_VALUE + " = new java.util.ArrayList<{3}>(); \n" +
        "'}' \n" +
        "for (java.util.Map.Entry<{4},{5}> $lombokEntry : {0}.entrySet()) '{'\n" +
        "this.{0}" + LOMBOK_KEY + ".add($lombokEntry.getKey());\n" +
        "this.{0}" + LOMBOK_VALUE + ".add($lombokEntry.getValue());\n" +
        "'}'{1}";

    final PsiType keyType = PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 0);
    final PsiType valueType = PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 1);

    final PsiType keyIterType = PsiTypeUtil.extractAllElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 0);
    final PsiType valueIterType = PsiTypeUtil.extractAllElementType(psiFieldType, psiManager, CommonClassNames.JAVA_UTIL_MAP, 1);

    return MessageFormat.format(codeBlockTemplate, singularName, fluentBuilder ? "\nreturn this;" : "",
        keyType.getCanonicalText(false), valueType.getCanonicalText(false),
        keyIterType.getCanonicalText(false), valueIterType.getCanonicalText(false));
  }
}
