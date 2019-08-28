# ZBarScanner
* Android扫码Demo，基于ZBar（主要展示异步解码的处理方式，解码程序也可以换成ZXing）；
* 本项目只编写Java部分代码，编写的代码主要集中在com.myscanner目录下；

## 摄像机
* 自定义的摄像机，使用SurfaceView；
* 自定义了遮罩与扫码线框，在获取照相机图片后会截取线框内的图片分析；
* 摄像机在SurfaceView的surfaceCreated中初始化，在surfaceChanged刷新参数，在surfaceDestroyed中释放。
* 在onResume中设置SurfaceView为可见，防止黑屏。

## 多线程
* 使用异步解码，使用handler通讯，并注意防止内存泄漏。
* DecodeHandler为DecodeThread接收消息，处理的消息只有一种：解码。
* DecodeThread在启动同时也启动了Looper循环。
* DecodeThread内使用CountDownLatch，保证在线程启动后才能get到handler。
* ActivityHandler为主线程接收消息，处理的消息有：自动对焦、解码成功、解码结束。
* 解码成功后模拟处理信息，新启线程sleep两秒后发送解码结束消息给主线程。