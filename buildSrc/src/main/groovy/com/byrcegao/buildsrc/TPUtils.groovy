package com.byrcegao.buildsrc

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.NotFoundException
import javassist.bytecode.CodeAttribute
import javassist.bytecode.LocalVariableAttribute
import javassist.bytecode.MethodInfo
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

import java.lang.reflect.Modifier
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class TPUtils {
  private static ClassPool sPool = ClassPool.getDefault()
  private static final String ANNO_TAG = "DebugLogger"   //注解类名称

  /**
   * 注入耗时代码
   * @param path, jar包路径
   * @param project
   */
  static void injectTimeCode(String path, Project project, TPConfig config) {
    println("TPUtils add path:" + path)
    sPool.appendClassPath(path)
    project.android.bootClasspath.each {
      sPool.appendClassPath(it.absolutePath)
      println("TPUtils appendBootClassPath:" + it.absolutePath)  //android.jar的路径
    }

    File dir = new File(path)
    if (dir.isDirectory()) {
      dir.eachFileRecurse { File file ->
        String filePath = file.absolutePath

        //跳过as生成的中间类
        if (filePath.endsWith(".class") && !filePath.contains('R$') &&
            !filePath.contains('R.class') &&
            !filePath.contains('R2.class') &&
            !filePath.contains("BuildConfig.class")) {
          //请注意， debug或release方式编译的中间目录不同
          String classPath
          String[] classPathWindowsDebug = filePath.split("\\\\debug\\\\")  //windows格式
          String[] classPathMacDebug     = filePath.split("/debug/")    //mac格式
          String[] classPathWindowsRelease = filePath.split("\\\\release\\\\")  //windows格式
          String[] classPathMacRelease     = filePath.split("/release/")    //mac格式
          if (classPathWindowsDebug != null && classPathWindowsDebug.length == 2) {
            classPath = classPathWindowsDebug[1]
          }
          if (classPathMacDebug != null && classPathMacDebug.length == 2) {
            classPath = classPathMacDebug[1]
          }
          if (classPathWindowsRelease != null && classPathWindowsRelease.length == 2) {
            classPath = classPathWindowsRelease[1]
          }
          if (classPathMacRelease != null && classPathMacRelease.length == 2) {
            classPath = classPathMacRelease[1]
          }

          //获取 .class 前的类名
          println("类名" + classPath)
          String className = classPath.substring(0, classPath.length() - 6)
              .replace('\\', '.')
              .replace('/', '.')

          //配置是否显示日志
          if (config.showLog) {
            println("className:" + className)
          }

          CtClass ctClass = sPool.getCtClass(className)
          if (ctClass.isFrozen()) {
            ctClass.defrost()
          }

          def classAllInject = false
          if (ctClass.getAnnotations() != null) {
            for (int i=0; i<ctClass.getAnnotations().size(); i++) {
              if (ctClass.getAnnotations()[i].toString().contains(ANNO_TAG)) {
                classAllInject = true
                println("类注解")
              }
              println(ctClass.getAnnotations()[i])
            }
          }

          CtMethod[] methods = ctClass.getDeclaredMethods()
          for (CtMethod method:methods) {
            //跳过空函数体或者native方法
            println("遍历方法~~~~~~~~~~")
            if (method.isEmpty() || Modifier.isNative(method.getModifiers())) {
              println("遍历方法退出")
              continue
            }

            println("遍历方法继续")
            if (classAllInject) {
              if (ctClass.isFrozen()) {
                ctClass.defrost()
              }
              println(className + "， isfrozen：" + ctClass.isFrozen())
              injectMethodTimeCode(method, className)  //如果在类上面添加注解则所有方法要注入
            } else {
              def methodInject = false
              //判断方法的注解
              Object[] objects = method.getAvailableAnnotations()
              if (objects != null && objects.length > 0) {
                for (java.lang.Object object : objects) {
                  if (object.toString().contains(ANNO_TAG)) {
                    methodInject = true
                    break
                  }
                }
              }

              if (methodInject) {
                //注入代码
                if (ctClass.isFrozen()) {
                  ctClass.defrost()
                }
                println(className + "， isfrozen：" + ctClass.isFrozen())
                injectMethodTimeCode(method, className)
              }
            }
            ctClass.writeFile(path)
          }

          ctClass.detach()  //释放资源
        }

      }
    }
  }
  static List<String> getMethodParamNames(CtMethod cm) {
    List<String> array = new ArrayList<>()

    MethodInfo methodInfo = cm.getMethodInfo();
    CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
    LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
        .getAttribute(LocalVariableAttribute.tag);

    println("getMethodParamNames长度:" + cm.getParameterTypes().length)
    int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
    for (int i = 0; i < cm.getParameterTypes().length; i++) {
      println("getMethodParamNames:" + attr.variableName(i+pos))
      array.add(attr.variableName(i + pos))
    }
    return array
  }

  /**
   * 向方法注入代码
   * @param method
   */
  static void injectMethodTimeCode(CtMethod method, String className) {
    println("injectMethodTimeCode注入代码~~~~~~~")

    //所有函数参数名称
    List<String> paramNames = new ArrayList<>()
    MethodInfo methodInfo = method.getMethodInfo();
    CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
    LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
        .getAttribute(LocalVariableAttribute.tag);
    if (attr != null) {
      int len = method.getParameterTypes().length;
      int pos = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
      for (int i = 0; i <= len; i++) {
        try {
          println("参数名称：" + attr.variableName(i))
          if (!attr.variableName(i + pos).equals("this")) {
            paramNames.add(attr.variableName(i + pos))
          }
        } catch (Exception ex) {

        }
      }
    }

    println(method.methodInfo.name + " 参数总数：" + paramNames.size())
    def typeString = sPool.getCtClass("java.lang.String")
    method.addLocalVariable("startTime", CtClass.longType)
    method.addLocalVariable("endTime", CtClass.longType)
    method.addLocalVariable("classPath", typeString)
    method.addLocalVariable("methodName", typeString)

    //函数前注入
    StringBuilder builder = new StringBuilder()
    builder.append("  startTime = System.currentTimeMillis();\n")
    builder.append("  classPath = " + "\"" + className + "\";\n")
    builder.append("  methodName = " + "\"" + method.methodInfo.name + "\";\n")
    try {
      method.insertBefore(builder.toString())
    } catch (Exception ex) {
      ex.printStackTrace()
    }

    //函数尾部注入
    StringBuilder endBuilder = new StringBuilder()
    endBuilder.append(" endTime = System.currentTimeMillis();\n")
    endBuilder.append(" android.util.Log.d(\"MethodTime\", classPath + \":\" + methodName ")
    endBuilder.append(" + \"耗时：\"+ (endTime-startTime) + \"毫秒\");\n " )
    try {
      method.insertAfter(endBuilder.toString())
    } catch (Exception ex) {
      ex.printStackTrace()
    }

    //打印参数和值
    StringBuilder paramBuilder = new StringBuilder()
    paramBuilder.append(" android.util.Log.d(\"MethodTime\", classPath + \":\" + methodName ")
    for (int i=0; i<paramNames.size(); i++) {
      paramBuilder.append(" + ")
      paramBuilder.append("\"参数：\"")
      paramBuilder.append(" + ")
      paramBuilder.append("\"${paramNames[i]}\"")
      paramBuilder.append(" + ")
      paramBuilder.append("\":\"")
      paramBuilder.append(" + ")
      paramBuilder.append(paramNames[i])
      //paramBuilder.append("${i}")
    }
    paramBuilder.append(");\n")
    String str = paramBuilder.toString()
    println("字符串：" + str)
    try {
      method.insertAfter(str)
    } catch (Exception ex) {
      ex.printStackTrace()
    }
  }

  /**
   * 获取local.properties文件，得到sdk.dir属性值
   * @param project
   * @return android.jar的存储位置
   */
  static String getJarPostion(Project project) {
    def file = new File(project.rootDir, "local.properties")
    def sdkDir = null
    if (file.exists()) {
      Properties properties = new Properties()
      file.withInputStream { inputStream ->
        properties.load(inputStream)
      }
      sdkDir = properties.getProperty("sdk.dir")
    }

    // sdk/platforms目录下可能有多个android版本，例如android-24、android-25等，取最高版本的android.jar
    def platformFile = new File(sdkDir + File.separator + "platforms")
    if (platformFile.isDirectory()) {
      return platformFile.absolutePath + File.separator +
          platformFile.list().sort()[platformFile.list().size()-1] + File.separator
      + "android.jar"
    }
    return null
  }

  /**
   * 遍历jar的.class文件，并实现字节码注入
   * @param jarFile， 要注入字节码的jar
   * @param tempDir， 临时目录
   * @param hexName， md5值
   * @return
   */
  static File modifyJar(File jarFile, File tempDir, String hexName) {
    def file = new JarFile(jarFile)
    def outputJar = new File(tempDir, hexName + "_" + jarFile.name + ".tmp") //输出临时文件

    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream((outputJar)))
    Enumeration<JarEntry> enumeration = file.entries()   //遍历jar包所有.class文件

    while (enumeration.hasMoreElements()) {
      JarEntry jarEntry = enumeration.nextElement()
      if (jarEntry.isDirectory()) {
        //如果是目录则继续
        continue
      }

      String entryName = jarEntry.getName()
      ZipEntry zipEntry = new ZipEntry(entryName)
      jarOutputStream.putNextEntry(zipEntry)

      InputStream inputStream = file.getInputStream(jarEntry)
      byte[] modifiedClassBytes = null   //修改后的字节码
      byte[] sourceClassBytes   = IOUtils.toByteArray(inputStream)   //原始字节码
      if (entryName.endsWith(".class")) {
        println(entryName + " will change...")
        modifiedClassBytes = visitAndReturnCode(sourceClassBytes)
      }

      if (modifiedClassBytes == null || modifiedClassBytes.length == 0) {
        jarOutputStream.write(sourceClassBytes)
      } else {
        jarOutputStream.write(modifiedClassBytes)
      }

      jarOutputStream.closeEntry()
    }

    jarOutputStream.close()
    file.close()

    return outputJar
  }
}
