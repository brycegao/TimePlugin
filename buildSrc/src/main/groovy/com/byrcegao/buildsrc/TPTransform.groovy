package com.byrcegao.buildsrc

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.gradle.api.Project;

class TPTransform extends Transform {

  private Project mProject
  private TPConfig mConfig

  TPTransform(Project p, TPConfig config) {
    mProject = p
    mConfig = config
  }

  /**
   * Returns the unique name of the transform.
   *
   * <p/>
   * This is associated with the type of work that the transform does. It does not have to be
   * unique per variant.*/
  @Override
  String getName() {
    return "TPTransform"
  }

  /**
   * Returns the type(s) of data that is consumed by the Transform. This may be more than
   * one type.
   * <strong>This must be of type {@link com.android.build.api.transform.QualifiedContent.DefaultContentType}</strong>
   * Transform的输入类型*/
  @Override
  Set<QualifiedContent.ContentType> getInputTypes() {
    return TransformManager.CONTENT_CLASS
  }

  /**
   * Returns the scope(s) of the Transform. This indicates which scopes the transform consumes.
   * Transform的作用范围*/
  @Override
  Set<QualifiedContent.Scope> getScopes() {
    return TransformManager.SCOPE_FULL_PROJECT
  }

  @Override
  boolean isIncremental() {
    return false
  }

  @Override
  void transform(TransformInvocation transformInvocation)
      throws TransformException, InterruptedException, IOException {
    def outputProvider = transformInvocation.outputProvider

    transformInvocation.inputs.each { TransformInput input ->

      input.directoryInputs.each { DirectoryInput directoryInput ->
        TPUtils.injectTimeCode(directoryInput.file.getAbsolutePath(), mProject, mConfig)

        def dest = outputProvider.getContentLocation(directoryInput.name,
            directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)

        //将 input 的目录复制到 output 指定目录
        FileUtils.copyDirectory(directoryInput.file, dest)
      }


      input.jarInputs.each { JarInput jarInput ->
        TPUtils.injectTimeCode(jarInput.file.getAbsolutePath(), mProject, mConfig)

        //重命名输出文件（同目录 copyFile 会冲突）
        def jarName = jarInput.name
        def md5Name = jarInput.file.hashCode()
        if (jarName.endsWith(".jar")) {
          jarName = jarName.substring(0, jarName.length() - 4)
        }

        def dest = outputProvider.getContentLocation(jarName + md5Name,
            jarInput.contentTypes, jarInput.scopes, Format.JAR)
        FileUtils.copyFile(jarInput.file, dest)
      }
    }

  }
}
