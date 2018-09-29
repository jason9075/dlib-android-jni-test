# Android 即時人臉辨識 練習

[![Demo CountPages alpha](https://j.gifs.com/nrwwNp.gif)](https://youtu.be/edJH8bvdxBs)

參考 [此處](https://tech.pic-collage.com/face-landmarks-detection-in-your-android-app-part-1-2c4431eaa3d9) 所練習的小實做

主要學習 Android 如何利用 NDK 與 C++ 進行互動，並實測 dlib 跑在手機上的效率。
實作中，原始Dlib的臉部偵測效率並不好 大約每五秒偵測一張，但改使用 Google 內建 FaceDetector 效率極高，臉部68節點偵測的部份仍使用dlib來進行。

套件|版本
--- | ---
IDE|Android Studio 3.2 Preview
Dlib| 19.15
Cmake| 3.12.0
NDK| 18.0.4951716 rc2
ProtoBuff| 3.6.1

