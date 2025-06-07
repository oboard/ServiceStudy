# Android前台服务计数器应用开发文档

## 1. 项目环境搭建  
   新建Android项目，配置Kotlin + Jetpack Compose开发环境，并在build.gradle.kts中引入必要依赖：
   ```kotlin
   // 版本目录配置 (gradle/libs.versions.toml)
   lifecycleViewmodelCompose = "2.7.0"
   kotlinxCoroutines = "1.7.3"
   
   // 依赖引入 (app/build.gradle.kts)
   implementation(libs.androidx.lifecycle.viewmodel.compose)
   implementation(libs.kotlinx.coroutines.android)
   ```

## 2. 设计前台服务核心类  
   编写CounterService类，利用协程实现定时计数功能。核心代码如下：
   ```kotlin
   class CounterService : Service() {
       private var counter = 0
       private var serviceJob: Job? = null
       private val serviceScope = CoroutineScope(Dispatchers.Default)
       
       private fun startForegroundService() {
           serviceJob = serviceScope.launch {
               while (true) {
                   counter++
                   updateNotification()
                   broadcastCounterUpdate()
                   delay(1000) // 每秒更新一次
               }
           }
       }
   }
   ```
   通过广播机制实现服务与UI的实时通信，保证界面同步更新。

## 3. 实现服务控制逻辑  
   在MainViewModel中，设置服务控制方法，实现启动、停止等功能：
   ```kotlin
   fun startService() {
       val intent = Intent(getApplication(), CounterService::class.java).apply {
           action = CounterService.ACTION_START
       }
       getApplication<Application>().startService(intent)
       _isServiceRunning.value = true
   }
   
   fun stopService() {
       val intent = Intent(getApplication(), CounterService::class.java).apply {
           action = CounterService.ACTION_STOP
       }
       getApplication<Application>().startService(intent)
       _isServiceRunning.value = false
   }
   ```
   并通过StateFlow管理服务状态，实现响应式UI更新。

## 4. 前台服务集成  
   在AndroidManifest.xml中声明前台服务和权限：
   ```xml
   <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
   <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
   
   <service
       android:name=".CounterService"
       android:enabled="true"
       android:exported="false"
       android:foregroundServiceType="dataSync" />
   ```
   前台服务在通知栏显示持久通知，提升进程优先级，确保后台计数不中断。

## 5. UI设计与数据绑定  
   使用Jetpack Compose + Material3设计现代化界面：
   ```kotlin
   @Composable
   fun MainScreen(viewModel: MainViewModel = viewModel()) {
       val isServiceRunning by viewModel.isServiceRunning.collectAsState()
       val counter by viewModel.counter.collectAsState()
       
       Column {
           // 服务状态卡片
           Card(colors = CardDefaults.cardColors(
               containerColor = if (isServiceRunning) 
                   MaterialTheme.colorScheme.primaryContainer 
               else 
                   MaterialTheme.colorScheme.surfaceVariant
           )) {
               Text("Service Status: ${if (isServiceRunning) "Running" else "Stopped"}")
           }
           
           // 计数器显示卡片
           Card {
               Text(text = counter.toString(), style = MaterialTheme.typography.displayLarge)
           }
           
           // 控制按钮
           Button(onClick = { 
               if (isServiceRunning) viewModel.stopService() 
               else viewModel.startService() 
           }) {
               Text(if (isServiceRunning) "Stop Service" else "Start Service")
           }
       }
   }
   ```
   通过StateFlow实现数据响应式绑定，UI状态自动同步更新。

## 6. 生命周期管理  
   通过BroadcastReceiver和ViewModel生命周期绑定，实现资源的合理管理：
   ```kotlin
   class MainViewModel(application: Application) : AndroidViewModel(application) {
       private val counterReceiver = object : BroadcastReceiver() {
           override fun onReceive(context: Context?, intent: Intent?) {
               // 接收服务广播更新UI
           }
       }
       
       init {
           // 注册广播接收器
           getApplication<Application>().registerReceiver(counterReceiver, filter)
       }
       
       override fun onCleared() {
           super.onCleared()
           // 自动清理资源
           getApplication<Application>().unregisterReceiver(counterReceiver)
       }
   }
   ```
   同时配置Activity单例模式（`android:launchMode="singleTop"`），确保从通知栏返回时不创建重复实例。

## 技术特点

- **现代化架构**：使用Jetpack Compose + ViewModel + StateFlow构建响应式UI
- **前台服务**：确保后台运行稳定性，通过通知栏显示运行状态
- **生命周期管理**：自动资源清理，避免内存泄漏
- **Material3设计**：遵循最新设计规范，提供优秀用户体验
- **协程支持**：异步处理，保证UI流畅性

## 运行效果

- 启动服务后，通知栏显示计数器状态
- 界面实时显示计数值变化
- 支持后台运行，应用切换到后台时计数继续
- 服务状态通过颜色变化直观显示 