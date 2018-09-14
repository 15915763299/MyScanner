package com.myscanner.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;

/**
 * 声音工具
 */
@SuppressLint("UseSparseArrays")
public class SoundUtils {
	/**
	 * 上下文
	 */
	private Context context;
	/**
	 * 声音池
	 */
	private SoundPool soundPool;
	/**
	 * 添加的声音资源参数
	 */
	private HashMap<Integer, Integer> soundPoolMap;
	/**
	 * 声音音量类型，默认为多媒体
	 */
	private int soundVolType;
	/**
	 * 无限循环播放
	 */
	public static final int INFINITE_PLAY = -1;
	/**
	 * 单次播放
	 */
	public static final int SINGLE_PLAY = 0;
	/**
	 * 铃声音量
	 */
	public static final int RING_SOUND = AudioManager.STREAM_RING;
	/**
	 * 媒体音量
	 */
	public static final int MEDIA_SOUND = AudioManager.STREAM_MUSIC;

	/**
	 * 构造器内初始化
	 * @param context 上下文
	 * @param soundVolType 声音音量类型，默认为多媒体
	 */
	public SoundUtils(Context context, int soundVolType) {
		this.context = context;
		this.soundVolType = soundVolType;
		// 初始化声音池和声音参数map
		soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		soundPoolMap = new HashMap<>();
	}

	/**
	 * 添加声音文件进声音池
	 * @param order 所添加声音的编号，播放的时候指定
	 * @param soundRes 添加声音资源的id
	 */
	public void putSound(int order, int soundRes) {
		// 上下文，声音资源id，优先级
		soundPoolMap.put(order, soundPool.load(context, soundRes, 1));
	}

	/**
	 * 播放声音
	 * @param order 所添加声音的编号
	 * @param times 循环次数，0无不循环，-1无永远循环
	 */
	@SuppressWarnings("static-access")
	public void playSound(int order, int times) {
		// 实例化AudioManager对象
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if(am != null){
			// 返回当前AudioManager对象播放所选声音的类型的最大音量值
			float maxVolume = am.getStreamMaxVolume(soundVolType);
			// 返回当前AudioManager对象的音量值
			float currentVolume = am.getStreamVolume(soundVolType);
			// 比值
			float volumeRatio = currentVolume / maxVolume;
			soundPool.play(soundPoolMap.get(order), volumeRatio, volumeRatio, 1, times, 1);
		}
	}

	/**
	 * 设置 soundVolType 的值
	 */
	public void setSoundVolType(int soundVolType) {
		this.soundVolType = soundVolType;
	}
}