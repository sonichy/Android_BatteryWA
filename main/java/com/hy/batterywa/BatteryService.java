package com.hy.batterywa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class BatteryService extends Service {
	int pluggedlast = 0, plugged, statuslast = -1, i = 0, cp, level0 = 0, level1 = 0, level2 = 0, lbs, le,
			lcs, lce, level;
	Date date, CST, CET, CFT, BCT, BST, BET, time0, time1;
	long duration, CFD, BD, it0, tt0, uptime, BD1;
	boolean paused = true;
	String SCST = "", SCET = "", SCFT = "", SCFD = "", SD, s = "电量：", s1 = "充电计时：", SBD = "", SBST = "",
			CPUs, stemperature, FPBST = "data/data/com.hy.batterywa/files/BST", SBP = "", SBET = "",
			SBFT = "";
	SimpleDateFormat dateformat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
	SimpleDateFormat timeformat = new SimpleDateFormat("H时m分s秒");
	SimpleDateFormat timeformat1 = new SimpleDateFormat("HH:mm");
	Timer timer;
	float vl = 0, vlb = 0, vlc = 0;
	CharSequence contentText;

	@Override
	public void onCreate() {
		DBHelper.getInstance(this);
		timeformat.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));

	}

	@Override
	public void onStart(Intent intent, int startId) {
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		BatteryReceiver batteryReceiver = new BatteryReceiver();
		registerReceiver(batteryReceiver, intentFilter);
		// if (timer == null) {
		// timer = new Timer();
		// timer.scheduleAtFixedRate(new Refresh(), 0, 1000);
		// }
	}

	// class Refresh extends TimerTask {
	// @Override
	// public void run() {
	// // cp = CPUusage();
	// // Log.e("CPU", cp + "%");
	// MainApplication.setbmsg(s + "\n" + s1);
	// }
	// }

	private final Handler handler = new Handler();
	private final Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if (!paused) {
				this.update();
				handler.postDelayed(this, 1000);
			}
		}

		void update() {
			date = new Date();
			duration = date.getTime() - CST.getTime();
			s1 = "充电计时：" + timeformat.format(duration);
			MainApplication.setbmsg(s + "\n" + s1);
		}
	};

	class BatteryReceiver extends BroadcastReceiver {
		@TargetApi(21)
		@Override
		public void onReceive(Context context, Intent intent) {
			SBP = "";
			// if
			// (intent.getAction().equals("android.intent.action.BATTERY_CHANGED"))
			// {
			int status = intent.getIntExtra("status", 0);
			int health = intent.getIntExtra("health", 0);
			level = intent.getIntExtra("level", 0);
			int scale = intent.getIntExtra("scale", 100);
			int temperature = intent.getIntExtra("temperature", 0);
			if (temperature > 100) {
				stemperature = "" + temperature / 10.0;
			} else {
				stemperature = "" + temperature;
			}
			int voltage = intent.getIntExtra("voltage", 0);
			int current = 0;
			String technology = intent.getStringExtra("technology");
			plugged = intent.getIntExtra("plugged", 0);
			if (Build.VERSION.SDK_INT > 20) {
				BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
				int BPC = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
				int BPCC = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
				int BPCA = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
				int BPCN = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
				int BPEC = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
				SBP = "\nBATTERY_PROPERTY_CAPACITY:" + BPC + "\nBATTERY_PROPERTY_CHARGE_COUNTER:" + BPCC
						+ "\nBATTERY_PROPERTY_CURRENT_AVERAGE:" + BPCA + "\nBATTERY_PROPERTY_CURRENT_NOW:"
						+ BPCN + "\nBATTERY_PROPERTY_ENERGY_COUNTER:" + BPEC;
			}
			String statusString = "";
			date = new Date();
			BCT = date;
			level1 = level;
			uptime = getUptime();
			if (i == 0) {
				time0 = date;
				level0 = level;
				File file = new File(FPBST);
				Log.e("File:BST.exists?", file.exists() + "");
				if (file.exists()) {
					String SBST = ReadFile1("BST");
					Log.e("ReadFile:BST=", SBST);
					String[] bs = SBST.split(",");
					if (bs.length > 2) {
						lbs = Integer.parseInt(bs[1]);
					} else {
						lbs = 100;
					}
					if (uptime < 300000)
						lbs = level;
					Log.e("Battery Start Level", lbs + "");
					try {
						BST = dateformat.parse(bs[0]);
						BD = date.getTime() - BST.getTime();
						if (BD > uptime) {
							BST = date;
							try {
								SaveFile("BST", dateformat.format(BST) + "," + level);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else {
					BST = date;
				}
			}
			if (level1 != level0) {
				time1 = date;
				if (time1 != time0)
					vl = (float) (1000 * 60 * 60 * (level1 - level0)) / (time1.getTime() - time0.getTime());
				time0 = time1;
				level0 = level1;
			}
			i++;
			switch (status) {
			case BatteryManager.BATTERY_STATUS_UNKNOWN:
				statusString = "未知";
				break;
			case BatteryManager.BATTERY_STATUS_CHARGING:
				statusString = "充电中";
				break;
			case BatteryManager.BATTERY_STATUS_DISCHARGING:
				statusString = "放电中";
				break;
			case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
				statusString = "未充电";
				break;
			case BatteryManager.BATTERY_STATUS_FULL:
				statusString = "已充满";
				Log.e("statuslast", statuslast + "");
				if (statuslast != BatteryManager.BATTERY_STATUS_FULL && statuslast != -1) {
					CFT = date;
					SCFT = dateformat.format(CFT);
					CFD = date.getTime() - CST.getTime();
					SCFD = timeformat.format(CFD);
				}
				break;
			}

			// 电流
			String scurrent = "";
			File f;
			String fp = "/sys/class/power_supply/battery/";
			String fn;
			fn = fp + "BatteryAverageCurrent";// 红米1，金立GN205，佳域G3
			f = new File(fn);
			if (f.exists()) {
				scurrent = ReadFile(fn);
			} else {
				fn = fp + "batt_fuel_current";// 三星I8262D
				f = new File(fn);
				if (f.exists()) {
					scurrent = ReadFile(fn);
				} else {
					fn = fp + "batt_current";// HTC X315e
					f = new File(fn);
					if (f.exists()) {
						scurrent = ReadFile(fn);
					} else {
						fn = fp + "current_now";
						f = new File(fn);
						if (f.exists()) {
							// NOTE8702
							// String c = ReadFile(fn);
							// scurrent = c.substring(0, c.length() - 3);
							// ZTE N958St
							scurrent = ReadFile(fn);
						} else {
							scurrent = "0";
						}
					}
				}
			}
			current = Integer.parseInt(scurrent);
			// 电量
			String rls = "";
			fn = fp + "charge_full_design";// 红米1
			f = new File(fn);
			if (f.exists()) {
				String cfdm = ReadFile(fn);
				// Log.e("mAh", cfdm);
				String cfd = cfdm.substring(0, cfdm.length() - 3);
				int cfdi = Integer.parseInt(cfd);
				int rln = cfdi * level / 100;
				rls = rln + "/" + cfd + "mAh";
			}

			String healthString = "";
			switch (health) {
			case BatteryManager.BATTERY_HEALTH_UNKNOWN:
				healthString = "未知";
				break;
			case BatteryManager.BATTERY_HEALTH_GOOD:
				healthString = "好";
				break;
			case BatteryManager.BATTERY_HEALTH_OVERHEAT:
				healthString = "过热";
				break;
			case BatteryManager.BATTERY_HEALTH_DEAD:
				healthString = "损坏";
				break;
			case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
				healthString = "超压";
				break;
			case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
				healthString = "未知错误";
				break;
			}
			String acString = "";
			switch (plugged) {
			case BatteryManager.BATTERY_PLUGGED_AC:
				acString = "AC";
				break;
			case BatteryManager.BATTERY_PLUGGED_USB:
				acString = "USB";
				break;
			case BatteryManager.BATTERY_PLUGGED_WIRELESS:
				acString = "WIRELESS";
				break;
			}
			// 插上瞬间
			if (plugged != 0 && pluggedlast == 0) {
				CST = date;
				BET = date;
				SCST = dateformat.format(CST);
				SCET = "";
				SCFT = "";
				SCFD = "";
				lcs = level;
				paused = false;
				vl = 0;
				vlb = 0;
			}
			// 拔掉瞬间
			if (plugged == 0 && pluggedlast != 0) {
				lce = level;
				lbs = level;
				CET = date;
				BST = date;
				SCET = dateformat.format(CET);
				paused = true;
				// if (status != BatteryManager.BATTERY_STATUS_FULL)
				// vla = (float) (1000 * 60 * 60 * (le - ls))
				// / (CET.getTime() - CST.getTime());
				// WriteFile(FPBST, BST + "");
				String sf = "";
				try {
					sf = dateformat.format(BST) + "," + level;
					SaveFile("BST", sf);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.e("WriteFile:BST=", sf);
			}
			// 充电状态
			if (plugged != 0) {
				SBST = "";
				duration = date.getTime() - CST.getTime();
				SD = timeformat.format(duration);
				lce = level;
				handler.postDelayed(runnable, 1000);
				SBD = "";
				if (status != BatteryManager.BATTERY_STATUS_FULL) {
					vlc = (float) (1000 * 60 * 60 * (lce - lcs)) / duration;
					SBFT = EmptyFullTime(level, vlc, date);
					contentText = SBFT;
				}
				SBET = "";
			}
			// 电池供电
			if (plugged == 0) {
				SBST = dateformat.format(BST);
				BD = date.getTime() - BST.getTime();
				SBD = LongToTime(BD);
				if (current > 0)
					current = -current;
				if (BD != 0)
					vlb = (float) (1000 * 60 * 60 * (level - lbs)) / BD;
				SBET = EmptyFullTime(level, vlb, date);
				contentText = SBET;
			}
			showNotification(level, statusString, contentText);
			s = "变次：" + i + "\n电量：" + level + "%" + "\n状态：" + statusString + "\n健康：" + healthString + "\n电源："
					+ acString + "\n电压：" + voltage + "mV\n电流：" + current + "mA\n容量：" + rls + "\n温度："
					+ stemperature + "°C\n材料：" + technology + "\n电池启用：" + SBST + "\n电池已用：" + SBD + "\n电池启电："
					+ lbs + "%\n耗电速度：" + vlb + " 电量/小时\n" + SBET + "\n%1电量速度：" + vl + " 电量/小时\n起充电量：" + lcs
					+ "%\n终充电量：" + lce + "%\n起充时间：" + SCST + "\n终充时间：" + SCET + "\n充满时间：" + SCFT + "\n充满时长："
					+ SCFD + "\n充电速度：" + vlc + " 电量/小时\n" + SBFT + "\n开机时长：" + LongToTime(getUptime()) + SBP;
			// Message msg = new Message();
			// Bundle b = new Bundle();
			// b.putString("s", s);
			// msg.setData(b);
			// MainActivity.myHandler.sendMessage(msg);
			MainApplication.setbmsg(s + "\n" + s1);
			statuslast = status;
			pluggedlast = plugged;
			DBHelper helper = new DBHelper(getApplicationContext());
			ContentValues values = new ContentValues();
			values.put("time", dateformat.format(date));
			values.put("level", level);
			values.put("voltage", voltage);
			values.put("current", current);
			values.put("temperature", temperature);
			values.put("cpu", 0);
			helper.insert(values);
		}
		// }
	}

	// @SuppressWarnings("deprecation")
	private void showNotification(int level, String contentTitle, CharSequence contentText) {
		// NotificationManager NM = (NotificationManager)
		// getSystemService(NOTIFICATION_SERVICE);
		// CharSequence contentText = "当前电量：" + level + "%";
		// Notification notification;
		// if (Build.VERSION.SDK_INT < 12) {// <12
		// notification = new Notification(
		// MainApplication.batteryStateIcon[level], "",
		// System.currentTimeMillis());
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;
		// notification.flags |= Notification.FLAG_NO_CLEAR;
		// Intent notificationIntent = new Intent(BatteryService.this,
		// MainActivity.class);
		// PendingIntent contentItent = PendingIntent.getActivity(this, 0,
		// notificationIntent, 0);
		// notification.setLatestEventInfo(this, contentTitle, contentText,
		// contentItent);
		// } else {
		// RemoteViews view_custom = new RemoteViews(getPackageName(),
		// R.layout.view_custom);
		// view_custom.setImageViewResource(R.id.custom_icon,
		// MainApplication.batteryStateIcon[level]);
		// view_custom.setTextViewText(R.id.tv_custom_title, contentTitle);
		// view_custom.setTextViewText(R.id.tv_custom_content, contentText);
		// view_custom.setTextViewText(R.id.tv_custom_time,
		// timeformat1.format(System.currentTimeMillis()));
		// Intent intent = new Intent(BatteryService.this, MainActivity.class);
		// PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
		// intent, 0);
		// Notification notification = new Notification.Builder(this)
		// .setSmallIcon(MainApplication.batteryStateIcon[level]).setContentTitle(contentTitle)
		// .setContentText(contentText).setContent(view_custom).setContentIntent(pendingIntent).build();
		// Notification notification = new
		// Notification.Builder(this).setContentTitle(contentTitle)
		// .setContentText(contentText).setSmallIcon(android.R.drawable.stat_notify_sync)
		// .setLargeIcon(BitmapFactory.decodeResource(getResources(),
		// R.drawable.battery_9))
		// .setContentIntent(pendingIntent).build();
		// .setTicker(contentText)
		// .setLights(Color.WHITE, 100, Integer.MAX_VALUE)
		// .setDefaults(Notification.DEFAULT_SOUND)
		// .setVibrate(new long[] { 0, 100 });
		// notification.contentView.setImageViewResource(android.R.id.icon,
		// MainApplication.batteryStateIcon[level]);
		// notification.icon = R.drawable.gvi;
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;
		// notification.flags |= Notification.FLAG_NO_CLEAR;
		// notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		// notification.flags |= notification.FLAG_ONLY_ALERT_ONCE;
		// notification.ledARGB = Color.RED;
		// notification.ledOnMS = 300;
		// notification.ledOffMS = 300;
		// }
		// NM.notify(0, notification);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(MainApplication.batteryStateIcon[level])
				.setContentTitle(level + "% " + contentTitle).setContentText(contentText);
		Intent resultIntent = new Intent(this, MainActivity.class);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = mBuilder.build();
		notification.flags = Notification.FLAG_NO_CLEAR;
		mNotifyMgr.notify(0, notification);
	}

	private void clear() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(0);
	}

	@Override
	public void onDestroy() {
		clear();
		super.onDestroy();
	}

	String ReadFile(String fp) {
		String s = "";
		try {
			s = new BufferedReader(new InputStreamReader(Runtime.getRuntime()
					.exec(new String[] { "cat", fp }).getInputStream())).readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return s;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	void WriteFile(String fp, String s) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fp, false)));
			bw.write(s);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	String ReadFile1(String filename) {
		String s = "";
		FileInputStream istream;
		try {
			istream = openFileInput(filename);
			byte[] buffer = new byte[istream.available()];
			ByteArrayOutputStream ostream = new ByteArrayOutputStream();
			int len;
			while ((len = istream.read(buffer)) != -1) {
				ostream.write(buffer, 0, len);
			}
			s = new String(ostream.toByteArray());
			istream.close();
			ostream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return s;
	}

	void SaveFile(String filename, String content) throws IOException {
		FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
		fos.write(content.getBytes());
		fos.close();
	}

	long getUptime() {
		FileReader fr = null;
		try {
			fr = new FileReader("/proc/uptime");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return 0;
		}
		BufferedReader br = new BufferedReader(fr);
		String s = "";
		try {
			s = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		// Log.e("uptime", s);
		long ms = new Double(Double.parseDouble(s.substring(0, s.indexOf(" "))) * 1000).longValue();
		return ms;
	}

	String LongToTime(long l) {
		String st = timeformat.format(l);
		if (l >= 86400000)
			st = l / 86400000 + "天" + st;
		return st;
	}

	String EmptyFullTime(int level, float v, Date date) {
		String st = "";
		long ms;
		float h;
		if (v < 0) {
			h = -level / v;
			ms = (long) (h * 3600000);
			String EmptyTime = dateformat.format(date.getTime() + ms);
			st = LongToTime(ms) + "后" + EmptyTime + "耗尽";
		}
		if (v > 0) {
			h = (100 - level) / v;
			ms = (long) (h * 3600000);
			String FullTime = dateformat.format(date.getTime() + ms);
			st = LongToTime(ms) + "后" + FullTime + "充满";
		}
		return st;
	}
}