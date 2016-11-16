# PaintView

[![Library Release](https://img.shields.io/badge/release-v1.1.3-green.svg)](https://github.com/LiuHongtao/PaintView)
[![MIT License](http://img.shields.io/:license-MIT-blue.svg)](https://github.com/LiuHongtao/PaintView/blob/master/LICENSE)
[![Android API](https://img.shields.io/badge/Android_API-9%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=9)

PaintView实现了在图片上涂鸦（也可以不在图片上），支持缩放和拖拽手势，并且能够获得并分享涂鸦结果图。

点击下方ICON下载Demo。

[![ICON](ic_launcher.png)](paintview_demo.apk)

## Gradle依赖

添加下方代码到工程build.gradle文件（不是Module的build.gradle文件）：

	allprojects {
	    repositories {
	        ...
	        jcenter()
	    }
	}

然后添加下方代码到Module的build.gradle文件：

	dependencies {
	    compile 'com.lht:paintview:{latest.release.version}'
	}

## 截图和已实现功能

![screenshot](screenshot.png)

### 1.1.3

* Bug修复

### 1.1

* 添加文本
* 缩放和拖拽手势

### 1.0

* 背景图设置
* 设置笔迹的颜色和宽度
* 撤销
* 获取涂鸦结果

## TODO

* 橡皮擦
* 画布旋转
* 注释和翻译

## Demo描述

* 将网页截图设置为背景图进行涂鸦
* 涂鸦完成后点击右上角分享
* 截图后Bitmap理论上可以通过Intent传递，但图片过大会导致崩溃，因此Demo中使用文件存储传递