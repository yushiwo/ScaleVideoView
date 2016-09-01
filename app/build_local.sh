#!/bin/sh
########################################
#@author hzzhengrui
#@description 实现模块构件和jar包合并  
########################################

#使用Gradle编译各个module
gradle clean
gradle build
# ./gradlew build --stacktrace --debug
 
#进入输出目录
cd build
cd outputs
 
#清空输出目录
rm -rf  *
 
#创建输出子目录
mkdir temp
mkdir debug
mkdir release
 
#定义sdk版本号
version="1.0.0"

 
#定义模块是否打包标识
is_include_src = true
is_include_ijkmediaplayer = true
is_include_fastjson = true
is_include_libraryvolley = true
 
#解压所有debug版本的jar包到temp目录中
cd temp

# 复制src
if $is_include_src; then
	cp -r ../../../build/intermediates/classes/debug/com .
	rm -rf com/netease/qingguo/demo
fi

# 解压jar
if $is_include_ijkmediaplayer; then
	jar -xvf ../../../libs/ijkmediaplayer.jar
fi

 
#压缩所有debug版本的class文件到一个独立的jar包中
jar -cvfM QGVideoView_${version}_debug.jar .
#普通jar转dex jar
dx --dex --output=QGVideoView_${version}_debug_dex.jar QGVideoView_${version}_debug.jar
 
#拷贝文件
mv QGVideoView_${version}_debug.jar ../debug
mv QGVideoView_${version}_debug_dex.jar ../debug

 

################################################################
#开始打release包
#清空temp目录
rm -rf *

# 复制src
if $is_include_src; then
	cp -r ../../../build/intermediates/classes/debug/com .
	rm -rf com/netease/qingguo/demo
fi

# 解压jar
if $is_include_ijkmediaplayer; then
	jar -xvf ../../../libs/ijkmediaplayer.jar
fi


#压缩所有debug版本的class文件到一个独立的jar包中
jar -cvfM QGVideoView_${version}_release.jar .
#普通jar转dex jar
dx --dex --output=QGVideoView_${version}_release_dex.jar QGVideoView_${version}_release.jar
 
#拷贝文件
mv QGVideoView_${version}_release.jar ../release
mv QGVideoView_${version}_release_dex.jar ../release

#删除temp目录
cd ..
rm -rf temp
 

