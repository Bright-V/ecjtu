package cn.edu.ecjtu;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.provider.Settings;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.MediaStore;
import android.app.Activity;
import java.io.OutputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.nio.charset.StandardCharsets;
import android.view.Gravity;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private static final int BG=Color.rgb(13,30,53), CARD=Color.rgb(26,50,82), BLUE=Color.rgb(45,139,255), MUTED=Color.rgb(157,176,201);
    private LinearLayout homeView,browserView; private WebView webView; private SwipeRefreshLayout homeRefresh; private int backgroundRefreshPending=0; private boolean backgroundRefreshing=false; private int backgroundRefreshFailures=0; private boolean setupInProgress=false; private TextView browserTitle; private ProgressBar progress; private TextView scheduleValue,scheduleDateLabel,powerValue,cardValue,spendValue,hitokotoValue,hitokotoFrom; private LinearLayout scheduleDayStrip; private String[] scheduleDays=new String[31]; private int selectedScheduleIndex=15; private HorizontalScrollView scheduleDayScroll; private android.content.SharedPreferences prefs; private static final int REQ_RESTORE_SESSION=4102; private String pendingBackupPassword=null; private String lastJwxtHtml=""; private String todaySummary="尚未同步课表"; private boolean returnAfterLogin=false;
    private static final String JWXT_OAUTH="https://mobile.ecjtu.edu.cn/oauth/authorize?client_id=pqZ3wGM07i8R9mR3&response_type=code&scope=base_api&state=ecjtu&redirect_uri=https%3A%2F%2Fmobileapp.ecjtu.edu.cn%2Fcheck%2Fredirectapp%3Fredirect_url%3Dhttps%253A%252F%252Fjwxt.ecjtu.edu.cn%252FstuMag%252FLogin_dcpLogin.action";
    private static final String BILLSYS_OAUTH="https://mobile.ecjtu.edu.cn/oauth/authorize?client_id=8fa4065773ff0df4&redirect_uri=http%3A%2F%2Fbillsys.ecjtu.edu.cn%2Fstudent%2F%23%2Fpages%2Fbindinfo%2Fbindinfo&response_type=code&scope=base_api&state=zhjdapp";
    private final Set<String> allowed=new HashSet<>(Arrays.asList("portal.ecjtu.edu.cn","cas.ecjtu.edu.cn","mobile.ecjtu.edu.cn","mobileapp.ecjtu.edu.cn","jwxt.ecjtu.edu.cn","ecard.ecjtu.edu.cn","billsys.ecjtu.edu.cn","atrustweb.ecjtu.edu.cn","xymjxt.ecjtu.edu.cn","zhxg.ecjtu.edu.cn","zhxgapi.ecjtu.edu.cn"));

    @Override public void onCreate(Bundle b){super.onCreate(b);WindowCompat.setDecorFitsSystemWindows(getWindow(),true);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.rgb(18,39,66));CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().flush();prefs=getSharedPreferences("local_session",MODE_PRIVATE);if(prefs.getBoolean("setupComplete",false))buildHome();else buildSetup();getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){public void handleOnBackPressed(){if(browserView!=null&&browserView.getVisibility()==View.VISIBLE){if(webView.canGoBack())webView.goBack();else showHome();}else finish();}});}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView t(String s,float z,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(color);v.setPadding(dp(4),dp(5),dp(4),dp(5));return v;}
    private TextView title(String s,float z){TextView v=t(s,z,Color.WHITE);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private void addSpace(LinearLayout p,int h){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    private void applyPagePadding(View root,int left,int top,int right,int bottom){root.setPadding(left,top,right,bottom);}

    private void buildSetup(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setBackgroundColor(BG);applyPagePadding(root,dp(28),dp(30),dp(28),dp(30));
        TextView mark=title("ECJTUEE",34);mark.setGravity(Gravity.CENTER);root.addView(mark,new LinearLayout.LayoutParams(-1,-2));
        TextView hello=title("欢迎使用",28);hello.setGravity(Gravity.CENTER);hello.setPadding(0,dp(36),0,dp(8));root.addView(hello);
        TextView intro=t("像设置一台新设备一样，只需完成一次校园账号登录。登录状态将由 Android WebView 保存在本机，ECJTUEE 不会保存你的账号密码，也不会上传登录信息。",15,MUTED);intro.setGravity(Gravity.CENTER);root.addView(intro,new LinearLayout.LayoutParams(-1,-2));
        Space flex=new Space(this);root.addView(flex,new LinearLayout.LayoutParams(1,0,1));
        LinearLayout info=card();info.addView(title("设置校园服务",19));info.addView(t("登录后将用于课表、宿舍电费、校园卡等校内服务。后续可直接使用已有的本机登录状态。",14,MUTED));root.addView(info,new LinearLayout.LayoutParams(-1,-2));addSpace(root,18);
        LinearLayout semester=card();semester.addView(title("开学日期设定",18));semester.addView(t("设置本学期第一周周一的日期，首页将自动显示当前周次。",13,MUTED));Button semesterBtn=new Button(this);semesterBtn.setText(prefs.getString("semesterStart","").length()>0?"已设置："+prefs.getString("semesterStart",""):"设置开学日期");semesterBtn.setAllCaps(false);semesterBtn.setTextColor(Color.WHITE);semesterBtn.setBackground(bg(CARD,12));semesterBtn.setOnClickListener(v->chooseSemesterStart());semester.addView(semesterBtn,new LinearLayout.LayoutParams(-1,dp(50)));root.addView(semester);addSpace(root,18);
        Button login=new Button(this);login.setText("登录校园账号");login.setAllCaps(false);login.setTextColor(Color.WHITE);login.setTextSize(16);login.setBackground(bg(BLUE,16));login.setOnClickListener(v->{setupInProgress=true;open("首次设置 · 登录",JWXT_OAUTH);});root.addView(login,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView note=t("继续即表示你将前往学校官方统一身份认证页面。",12,MUTED);note.setGravity(Gravity.CENTER);note.setPadding(0,dp(14),0,0);root.addView(note);
        setContentView(root);
    }
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(bg(CARD,18));return c;}
    private Button action(String label,String url){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setBackground(bg(CARD,14));b.setOnClickListener(v->open(label,url));return b;}

    private void buildHome(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);applyPagePadding(root,0,dp(30),0,0);
        ScrollView scroll=new ScrollView(this);homeView=new LinearLayout(this);homeView.setOrientation(LinearLayout.VERTICAL);homeView.setPadding(dp(20),dp(18),dp(20),dp(32));scroll.addView(homeView); homeRefresh=new SwipeRefreshLayout(this); homeRefresh.addView(scroll); homeRefresh.setOnRefreshListener(this::refreshAllHomeInBackground);

        Calendar cal=Calendar.getInstance();int h=cal.get(Calendar.HOUR_OF_DAY);String greet=h<6?"夜深了":h<11?"早上好":h<14?"中午好":h<18?"下午好":"晚上好";int week=getSchoolWeek(cal);String weekText=week>0?"第"+week+"周":"未设置周次";
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.HORIZONTAL);hero.setGravity(Gravity.BOTTOM);
        TextView greetView=title(greet+"，",31);hero.addView(greetView,new LinearLayout.LayoutParams(0,-2,1));
        TextView weekView=t(weekText,15,MUTED);weekView.setGravity(Gravity.RIGHT|Gravity.BOTTOM);weekView.setPadding(dp(4),dp(5),0,dp(8));hero.addView(weekView,new LinearLayout.LayoutParams(-2,-2));homeView.addView(hero);
        hitokotoValue=t(prefs.getString("hitokoto","正在获取一言…"),15,MUTED);hitokotoValue.setPadding(dp(4),dp(5),dp(4),0);homeView.addView(hitokotoValue);
        String cachedFrom=prefs.getString("hitokoto_from","");boolean hasFrom=cachedFrom!=null&&!cachedFrom.trim().isEmpty()&&!cachedFrom.trim().equalsIgnoreCase("null")&&!cachedFrom.trim().equals("—— null");hitokotoFrom=t(hasFrom?cachedFrom:"",12,MUTED);hitokotoFrom.setPadding(dp(4),dp(2),dp(4),dp(5));hitokotoFrom.setVisibility(hasFrom?View.VISIBLE:View.GONE);homeView.addView(hitokotoFrom);
        fetchHitokotoIfNeeded();
        addSpace(homeView,18);

        LinearLayout schedule=card();schedule.setTag("schedule");
        LinearLayout sh=new LinearLayout(this);sh.setGravity(Gravity.CENTER_VERTICAL);scheduleDateLabel=title("",20);sh.addView(scheduleDateLabel,new LinearLayout.LayoutParams(0,-2,1));Button all=action("查看课表",JWXT_OAUTH);all.setOnClickListener(v->open("智慧教务",JWXT_OAUTH,false));sh.addView(all,new LinearLayout.LayoutParams(dp(105),dp(46)));schedule.addView(sh);
        String cachedSchedule=prefs.getString("schedule",todaySummary);if("个人课表".equals(cachedSchedule.trim())){cachedSchedule="今日无事";prefs.edit().putString("schedule",cachedSchedule).apply();}
        boolean showMultiDay=prefs.getBoolean("showMultiDaySchedule",true);
        if(showMultiDay){
            scheduleDayScroll=new HorizontalScrollView(this);scheduleDayScroll.setHorizontalScrollBarEnabled(false);scheduleDayScroll.setFillViewport(false);scheduleDayStrip=new LinearLayout(this);scheduleDayStrip.setOrientation(LinearLayout.HORIZONTAL);scheduleDayScroll.addView(scheduleDayStrip);LinearLayout.LayoutParams dayLp=new LinearLayout.LayoutParams(-1,dp(64));dayLp.topMargin=dp(8);schedule.addView(scheduleDayScroll,dayLp);
            selectedScheduleIndex=15;String cachedDays=prefs.getString("scheduleDays","");if(!cachedDays.isEmpty()){String[] a=cachedDays.split(String.valueOf((char)30),-1);if(a.length==31)scheduleDays=a;else if(a.length==7){Calendar base=Calendar.getInstance();for(int i=0;i<31;i++){Calendar d=(Calendar)base.clone();d.add(Calendar.DAY_OF_MONTH,i-15);scheduleDays[i]=a[dayOfWeekIndex(d)];}}}
            scheduleValue=title(cachedDays.isEmpty()?cachedSchedule:scheduleDays[selectedScheduleIndex],17);scheduleValue.setLineSpacing(dp(6),1.08f);scheduleValue.setPadding(dp(4),dp(10),dp(4),dp(8));schedule.addView(scheduleValue);schedule.addView(t("左右滑动日期条，或在课程内容上左右滑动，查看前后 15 天。",13,MUTED));renderScheduleDayStrip();updateScheduleDayDisplay();
            scheduleValue.setOnTouchListener(new View.OnTouchListener(){float downX,downY;public boolean onTouch(View v,android.view.MotionEvent e){if(e.getAction()==android.view.MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();}else if(e.getAction()==android.view.MotionEvent.ACTION_UP){float dx=e.getX()-downX,dy=e.getY()-downY;if(Math.abs(dx)>dp(55)&&Math.abs(dx)>Math.abs(dy))selectScheduleDay(selectedScheduleIndex+(dx<0?1:-1));}return false;}});
        }else{
            scheduleDateLabel.setText("今天 · "+new SimpleDateFormat("M月d日 EEEE",Locale.CHINA).format(new Date()));
            scheduleValue=title(cachedSchedule,17);scheduleValue.setLineSpacing(dp(3),1.05f);schedule.addView(scheduleValue);
        }
        homeView.addView(schedule);addSpace(homeView,20);

        LinearLayout ah=new LinearLayout(this);ah.setGravity(Gravity.CENTER_VERTICAL);ah.addView(title("校园账户",22),new LinearLayout.LayoutParams(0,-2,1));TextView sync=t("点击卡片进入官方服务",13,MUTED);ah.addView(sync);homeView.addView(ah);addSpace(homeView,8);

        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout power=card(); power.setClickable(true); power.setFocusable(true); power.setOnClickListener(v->open("宿舍电费","https://billsys.ecjtu.edu.cn/student/#/"));
        power.addView(t("⚡  宿舍剩余电费",15,Color.WHITE)); powerValue=title(prefs.getString("power","未同步"),24); powerValue.setPadding(dp(4),dp(12),dp(4),0); power.addView(powerValue);
        power.addView(t("点击进入宿舍电费",13,Color.rgb(85,157,255)));
        LinearLayout money=accountCard("▣  校园卡余额",prefs.getString("card","未同步"),"消费 / 充值","https://ecard.ecjtu.edu.cn/"); cardValue=(TextView)money.getChildAt(1);
        row.addView(power,new LinearLayout.LayoutParams(0,dp(150),1));Space gap=new Space(this);row.addView(gap,new LinearLayout.LayoutParams(dp(10),1));row.addView(money,new LinearLayout.LayoutParams(0,dp(150),1));homeView.addView(row);addSpace(homeView,10);
        LinearLayout spend=accountCard("▥  本月支出",prefs.getString("spend","未同步"),"校园卡消费汇总","https://ecard.ecjtu.edu.cn/"); spendValue=(TextView)spend.getChildAt(1);homeView.addView(spend,new LinearLayout.LayoutParams(-1,dp(138)));addSpace(homeView,20);

        homeView.addView(title("快捷功能",20));addSpace(homeView,8);
        LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.addView(action("校园码","https://xymjxt.ecjtu.edu.cn/"),new LinearLayout.LayoutParams(0,dp(58),1));
        Space qg=new Space(this);quick.addView(qg,new LinearLayout.LayoutParams(dp(10),1));
        quick.addView(action("智慧教务",JWXT_OAUTH),new LinearLayout.LayoutParams(0,dp(58),1));homeView.addView(quick);addSpace(homeView,10);
        homeView.addView(t("\n登录信息仅保存在本机 WebView Cookie 和应用本地偏好中，不上传到第三方。",12,MUTED));

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(12),dp(6),dp(12),dp(8));nav.setBackgroundColor(Color.rgb(18,39,66));
        Button n1=navButton("⌂\n首页",true),n2=navButton("▦\n功能",false),n3=navButton("⚙\n设置",false);
        n1.setOnClickListener(v->{});n2.setOnClickListener(v->open("智慧教务",JWXT_OAUTH));n3.setOnClickListener(v->buildSettings());
        nav.addView(n1,new LinearLayout.LayoutParams(0,dp(72),1));nav.addView(n2,new LinearLayout.LayoutParams(0,dp(72),1));nav.addView(n3,new LinearLayout.LayoutParams(0,dp(72),1));
        root.addView(homeRefresh,new LinearLayout.LayoutParams(-1,0,1));root.addView(nav);setContentView(root);
        homeRefresh.post(()->{if(!backgroundRefreshing){homeRefresh.setRefreshing(true);refreshAllHomeInBackground();}});
    }

    private int dayOfWeekIndex(Calendar date){int d=date.get(Calendar.DAY_OF_WEEK);return d==Calendar.SUNDAY?6:d-2;}
    private String relativeDayLabel(int offset){if(offset==0)return "今天";if(offset==1)return "明天";if(offset==2)return "后天";if(offset==-1)return "昨天";if(offset==-2)return "前天";return offset>0?offset+"天后":(-offset)+"天前";}
    private Calendar scheduleDateAt(int index){Calendar d=Calendar.getInstance();d.set(Calendar.HOUR_OF_DAY,0);d.set(Calendar.MINUTE,0);d.set(Calendar.SECOND,0);d.set(Calendar.MILLISECOND,0);d.add(Calendar.DAY_OF_MONTH,index-15);return d;}
    private void renderScheduleDayStrip(){if(scheduleDayStrip==null)return;scheduleDayStrip.removeAllViews();for(int i=0;i<31;i++){Calendar d=scheduleDateAt(i);String label=relativeDayLabel(i-15)+"\n"+new SimpleDateFormat("M月d日 EEEE",Locale.CHINA).format(d.getTime());TextView b=new TextView(this);b.setText(label);b.setGravity(Gravity.CENTER);b.setTextSize(11);b.setTextColor(i==selectedScheduleIndex?Color.WHITE:MUTED);b.setBackground(bg(i==selectedScheduleIndex?BLUE:Color.rgb(18,39,66),12));b.setPadding(dp(8),dp(5),dp(8),dp(5));b.setMinWidth(dp(112));b.setMinHeight(dp(58));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(58));lp.rightMargin=dp(7);scheduleDayStrip.addView(b,lp);final int index=i;b.setOnClickListener(v->selectScheduleDay(index));}if(scheduleDayScroll!=null)scheduleDayScroll.post(()->{View selected=scheduleDayStrip.getChildAt(selectedScheduleIndex);if(selected!=null)scheduleDayScroll.smoothScrollTo(Math.max(0,selected.getLeft()-(scheduleDayScroll.getWidth()-selected.getWidth())/2),0);});}
    private void updateScheduleDayDisplay(){if(scheduleDateLabel==null||scheduleValue==null)return;Calendar d=scheduleDateAt(selectedScheduleIndex);int offset=selectedScheduleIndex-15;scheduleDateLabel.setText(relativeDayLabel(offset)+"\n"+new SimpleDateFormat("M月d日 EEEE",Locale.CHINA).format(d.getTime()));scheduleDateLabel.setLineSpacing(dp(2),1.0f);String value=scheduleDays[selectedScheduleIndex];if(value==null||value.isEmpty())value=offset==0?prefs.getString("schedule","尚未同步课表"):"暂无课程";scheduleValue.setText(value);}
    private void selectScheduleDay(int index){if(index<0||index>30)return;selectedScheduleIndex=index;renderScheduleDayStrip();updateScheduleDayDisplay();}
    private boolean courseOccursInWeek(String line,int weekNo){
        if(weekNo<=0)return true;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:^|\\\\s)(\\\\d+(?:-\\\\d+)?(?:,\\\\d+(?:-\\\\d+)?)*)\\\\s+[\\\\d,]+$").matcher(line.trim());
        if(!m.find())return true;
        String[] ranges=m.group(1).split(",");
        for(String part:ranges){try{String[] ends=part.split("-");int first=Integer.parseInt(ends[0]);int last=ends.length>1?Integer.parseInt(ends[1]):first;if(weekNo>=first&&weekNo<=last)return true;}catch(Exception ignored){}}
        return false;
    }
    private int getSchoolWeek(Calendar today){
        String start=prefs.getString("semesterStart","");
        if(start.length()==0)return 0;
        try{
            Calendar first=Calendar.getInstance();
            first.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(start));
            first.set(Calendar.HOUR_OF_DAY,0);first.set(Calendar.MINUTE,0);first.set(Calendar.SECOND,0);first.set(Calendar.MILLISECOND,0);
            Calendar cur=(Calendar)today.clone();
            cur.set(Calendar.HOUR_OF_DAY,0);cur.set(Calendar.MINUTE,0);cur.set(Calendar.SECOND,0);cur.set(Calendar.MILLISECOND,0);
            long days=(cur.getTimeInMillis()-first.getTimeInMillis())/86400000L;
            return days<0?0:(int)(days/7)+1;
        }catch(Exception e){return 0;}
    }
    private void chooseSemesterStart(){
        final EditText input=new EditText(this);input.setHint("例如 2026-09-07");input.setSingleLine(true);input.setInputType(android.text.InputType.TYPE_CLASS_DATETIME);
        String old=prefs.getString("semesterStart","");if(old.length()>0)input.setText(old);
        int p=dp(20);FrameLayout box=new FrameLayout(this);box.setPadding(p,0,p,0);box.addView(input,new FrameLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setTitle("开学日期设定").setMessage("设置本学期第一周周一的日期，首页会自动计算当前周次。").setView(box).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{
            String value=input.getText().toString().trim();
            try{new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(value);prefs.edit().putString("semesterStart",value).apply();Toast.makeText(this,"开学日期已保存",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"日期格式应为 YYYY-MM-DD",Toast.LENGTH_SHORT).show();}
        }).setNeutralButton("清除",(d,w)->{prefs.edit().remove("semesterStart").apply();Toast.makeText(this,"开学日期已清除",Toast.LENGTH_SHORT).show();}).show();
    }

    private void fetchHitokotoIfNeeded(){
        String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
        if(today.equals(prefs.getString("hitokoto_date",""))&&prefs.getString("hitokoto","").length()>0)return;
        fetchHitokoto(today);
    }
    private void fetchHitokoto(String today){
        new Thread(()->{
            java.net.HttpURLConnection conn=null;
            try{
                java.net.URL url=new java.net.URL("https://v1.hitokoto.cn/?encode=json");
                conn=(java.net.HttpURLConnection)url.openConnection();conn.setConnectTimeout(5000);conn.setReadTimeout(5000);conn.setRequestMethod("GET");conn.setRequestProperty("Accept","application/json");
                if(conn.getResponseCode()!=200)throw new java.io.IOException("HTTP "+conn.getResponseCode());
                java.io.BufferedReader br=new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(),StandardCharsets.UTF_8));StringBuilder raw=new StringBuilder();String line;while((line=br.readLine())!=null)raw.append(line);br.close();
                org.json.JSONObject o=new org.json.JSONObject(raw.toString());String quote=o.optString("hitokoto","").trim();String from=o.optString("from","").trim();String who=o.optString("from_who","").trim();
                if("null".equalsIgnoreCase(from))from="";if("null".equalsIgnoreCase(who))who="";if(quote.isEmpty())return;
                String source="";if(!who.isEmpty()&&!from.isEmpty())source="—— "+who+" · "+from;else if(!who.isEmpty())source="—— "+who;else if(!from.isEmpty())source="—— "+from;
                prefs.edit().putString("hitokoto",quote).putString("hitokoto_from",source).putString("hitokoto_date",today).apply();
                final String finalSource=source;runOnUiThread(()->{if(hitokotoValue!=null)hitokotoValue.setText(quote);if(hitokotoFrom!=null){hitokotoFrom.setText(finalSource);hitokotoFrom.setVisibility(finalSource.isEmpty()?View.GONE:View.VISIBLE);}});
            }catch(Exception ignored){}finally{if(conn!=null)conn.disconnect();}
        }).start();
    }

    private void buildSettings(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);applyPagePadding(root,0,dp(30),0,0);

        ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(18),dp(20),dp(32));scroll.addView(content);
        content.addView(title("设置",31));content.addView(t("管理 ECJTUEE 的本地数据与诊断",15,MUTED));addSpace(content,18);

        LinearLayout multiDay=card();
        LinearLayout multiDayRow=new LinearLayout(this);multiDayRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout multiDayText=new LinearLayout(this);multiDayText.setOrientation(LinearLayout.VERTICAL);multiDayText.addView(title("多日课表预览",18));multiDayText.addView(t("开启后可在首页切换查看前后日期；关闭后只显示今天的课程。",13,MUTED));
        multiDayRow.addView(multiDayText,new LinearLayout.LayoutParams(0,-2,1));
        Switch multiDaySwitch=new Switch(this);multiDaySwitch.setText("展示多日课表");multiDaySwitch.setTextColor(Color.WHITE);multiDaySwitch.setChecked(prefs.getBoolean("showMultiDaySchedule",true));
        multiDaySwitch.setOnCheckedChangeListener((button,checked)->{prefs.edit().putBoolean("showMultiDaySchedule",checked).apply();Toast.makeText(this,checked?"已开启多日课表":"已切换为仅显示今日课表",Toast.LENGTH_SHORT).show();});
        multiDayRow.addView(multiDaySwitch);multiDay.addView(multiDayRow);content.addView(multiDay);addSpace(content,12);

        LinearLayout reset=card();reset.setClickable(true);reset.setOnClickListener(v->confirmReset());reset.addView(title("初始化应用",18));reset.addView(t("清除本机登录状态、缓存的首页数据和首次设置状态，然后重新进入欢迎设置。",13,MUTED));content.addView(reset);addSpace(content,12);
        LinearLayout logs=card();logs.setClickable(true);logs.setOnClickListener(v->exportLogs());logs.addView(title("生成并保存日志",18));logs.addView(t("生成诊断日志并保存到手机 Downloads 目录。日志不会包含 Cookie、Token 或账号密码。",13,MUTED));content.addView(logs);addSpace(content,12);
        LinearLayout backup=card();backup.setClickable(true);backup.setOnClickListener(v->backupLoginState());backup.addView(title("备份",18));backup.addView(t("设置备份密码后，将登录状态加密导出到 Downloads。备份文件可用于换手机或卸载重装后恢复。",13,MUTED));content.addView(backup);addSpace(content,12);
        LinearLayout restore=card();restore.setClickable(true);restore.setOnClickListener(v->restoreLoginState());restore.addView(title("恢复",18));restore.addView(t("选择 .ecjtuee 备份文件并输入备份密码，恢复校园服务登录状态。",13,MUTED));content.addView(restore);

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(12),dp(6),dp(12),dp(8));nav.setBackgroundColor(Color.rgb(18,39,66));
        Button n1=navButton("⌂\n首页",false),n2=navButton("▦\n功能",false),n3=navButton("⚙\n设置",true);
        n1.setOnClickListener(v->showHome());n2.setOnClickListener(v->open("智慧教务",JWXT_OAUTH));n3.setOnClickListener(v->{});
        nav.addView(n1,new LinearLayout.LayoutParams(0,dp(72),1));nav.addView(n2,new LinearLayout.LayoutParams(0,dp(72),1));nav.addView(n3,new LinearLayout.LayoutParams(0,dp(72),1));

        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));root.addView(nav);setContentView(root);
    }
    private SecretKey passwordKey(char[] password,byte[] salt) throws Exception{
        PBEKeySpec spec=new PBEKeySpec(password,salt,150000,256);
        byte[] key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        spec.clearPassword();return new SecretKeySpec(key,"AES");
    }
    private String collectLoginCookies(){
        StringBuilder data=new StringBuilder();
        for(String host:allowed){String c=CookieManager.getInstance().getCookie("https://"+host+"/");if(c!=null&&!c.trim().isEmpty())data.append(host).append("\t").append(c).append("\n");}
        return data.toString();
    }
    private void backupLoginState(){
        final EditText pass=new EditText(this);pass.setHint("至少 8 位备份密码");pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int p=dp(20);FrameLayout box=new FrameLayout(this);box.setPadding(p,0,p,0);box.addView(pass,new FrameLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setTitle("创建可迁移备份").setMessage("备份包含登录 Cookie，请设置一个只有你知道的密码。换手机或重装后需要此密码恢复。").setView(box).setNegativeButton("取消",null).setPositiveButton("导出",(d,w)->{
            String password=pass.getText().toString();if(password.length()<8){Toast.makeText(this,"备份密码至少 8 位",Toast.LENGTH_LONG).show();return;}
            try{
                String data=collectLoginCookies();if(data.isEmpty()){Toast.makeText(this,"没有可备份的登录状态",Toast.LENGTH_SHORT).show();return;}
                byte[] salt=new byte[16],iv=new byte[12];SecureRandom sr=new SecureRandom();sr.nextBytes(salt);sr.nextBytes(iv);
                Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,passwordKey(password.toCharArray(),salt),new GCMParameterSpec(128,iv));
                byte[] enc=cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
                String name="ECJTUEE_login_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".ecjtuee";
                ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,name);cv.put(MediaStore.Downloads.MIME_TYPE,"application/octet-stream");cv.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);
                Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);if(uri==null)throw new IllegalStateException("create failed");
                try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(new byte[]{'E','C','J','B',1});out.write(salt);out.write(iv);out.write(enc);}
                Toast.makeText(this,"备份已保存到 Downloads/"+name,Toast.LENGTH_LONG).show();
            }catch(Exception e){Toast.makeText(this,"备份失败："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
        }).show();
    }
    private void restoreLoginState(){
        final EditText pass=new EditText(this);pass.setHint("备份密码");pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int p=dp(20);FrameLayout box=new FrameLayout(this);box.setPadding(p,0,p,0);box.addView(pass,new FrameLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setTitle("恢复登录状态").setMessage("先输入创建备份时设置的密码，然后选择 .ecjtuee 备份文件。").setView(box).setNegativeButton("取消",null).setPositiveButton("选择备份文件",(d,w)->{
            if(pass.getText().length()==0){Toast.makeText(this,"请输入备份密码",Toast.LENGTH_SHORT).show();return;}
            pendingBackupPassword=pass.getText().toString();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_RESTORE_SESSION);
        }).show();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=REQ_RESTORE_SESSION||resultCode!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        String password=pendingBackupPassword;pendingBackupPassword=null;if(password==null)return;
        try(InputStream in=getContentResolver().openInputStream(data.getData())){
            byte[] head=new byte[5];if(in.read(head)!=5||head[0]!='E'||head[1]!='C'||head[2]!='J'||head[3]!='B'||head[4]!=1)throw new IllegalStateException("format");
            byte[] salt=new byte[16],iv=new byte[12];if(in.read(salt)!=16||in.read(iv)!=12)throw new IllegalStateException("format");
            java.io.ByteArrayOutputStream buf=new java.io.ByteArrayOutputStream();byte[] tmp=new byte[4096];int n;while((n=in.read(tmp))>0)buf.write(tmp,0,n);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,passwordKey(password.toCharArray(),salt),new GCMParameterSpec(128,iv));
            String plain=new String(cipher.doFinal(buf.toByteArray()),StandardCharsets.UTF_8);
            CookieManager cm=CookieManager.getInstance();for(String line:plain.split("\n")){int q=line.indexOf('\t');if(q<=0)continue;String host=line.substring(0,q);if(!allowed.contains(host))continue;String cookies=line.substring(q+1);for(String c:cookies.split(";\\s*"))cm.setCookie("https://"+host+"/",c);}
            cm.flush();prefs.edit().putBoolean("setupComplete",true).apply();Toast.makeText(this,"登录状态已恢复，正在刷新",Toast.LENGTH_LONG).show();buildHome();
        }catch(javax.crypto.AEADBadTagException e){Toast.makeText(this,"恢复失败：密码错误或备份文件已损坏",Toast.LENGTH_LONG).show();}
        catch(Exception e){Toast.makeText(this,"恢复失败：备份文件无效",Toast.LENGTH_LONG).show();}
    }

    private void confirmReset(){
        new AlertDialog.Builder(this).setTitle("初始化 ECJTUEE").setMessage("将清除登录状态和本地缓存，并重新进入首次设置。确定继续吗？").setNegativeButton("取消",null).setPositiveButton("初始化",(d,w)->resetApp()).show();
    }
    private void resetApp(){
        prefs.edit().clear().apply();CookieManager.getInstance().removeAllCookies(v->{CookieManager.getInstance().flush();try{WebStorage.getInstance().deleteAllData();}catch(Exception ignored){}browserView=null;webView=null;buildSetup();Toast.makeText(this,"应用已初始化",Toast.LENGTH_SHORT).show();});
    }
    private void exportLogs(){
        try{
            File dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);if(!dir.exists())dir.mkdirs();
            String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());File out=new File(dir,"ECJTUEE_"+stamp+".log");
            String log="ECJTUEE diagnostic log\nGenerated: "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date())+"\nAndroid: "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\nDevice: "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+"\nApp: 0.1.0\nSetup complete: "+prefs.getBoolean("setupComplete",false)+"\nJWXT session detected: "+prefs.getBoolean("jwxtLogged",false)+"\nBillsys session detected: "+prefs.getBoolean("billsysLogged",false)+"\nEcard session detected: "+prefs.getBoolean("ecardLogged",false)+"\nLast cached schedule present: "+prefs.contains("schedule")+"\nLast cached power present: "+prefs.contains("power")+"\nLast cached card present: "+prefs.contains("card")+"\nNote: credentials, cookies and tokens are intentionally excluded.\n";
            try(FileOutputStream os=new FileOutputStream(out)){os.write(log.getBytes(StandardCharsets.UTF_8));}
            Toast.makeText(this,"日志已保存到 Downloads/"+out.getName(),Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"日志保存失败："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
    }

    private void refreshAllHomeInBackground(){
        if(backgroundRefreshing)return;
        backgroundRefreshing=true;backgroundRefreshPending=3;backgroundRefreshFailures=0;
        refreshHidden("https://jwxt.ecjtu.edu.cn/Schedule/Schedule_getUserSchedume.action?item=0205",0);
        refreshHidden("https://billsys.ecjtu.edu.cn/student/#/",1);
        refreshHidden("https://ecard.ecjtu.edu.cn/",2);
    }
    @SuppressLint("SetJavaScriptEnabled") private void refreshHidden(String url,int type){
        WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);CookieManager.getInstance().setAcceptThirdPartyCookies(w,true);
        final boolean[] finished={false};
        Runnable timeout=()->{if(finished[0])return;finished[0]=true;try{w.stopLoading();w.destroy();}catch(Exception ignored){}finishBackgroundRefresh(false);};
        w.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String h=r.getUrl().getHost();return h==null||!(allowed.contains(h)||h.endsWith(".ecjtu.edu.cn"));}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()&&!finished[0]){finished[0]=true;try{v.stopLoading();v.destroy();}catch(Exception ignored){}finishBackgroundRefresh(false);}}
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame()&&e.getStatusCode()>=400&&!finished[0]){finished[0]=true;try{v.stopLoading();v.destroy();}catch(Exception ignored){}finishBackgroundRefresh(false);}}
            @Override public void onPageFinished(WebView v,String u){
                if(finished[0])return;
                new android.os.Handler().postDelayed(()->{
                    if(finished[0])return;
                    String js=type==0?"(function(){var t=document.getElementById('courseSche');if(!t)return 'JWCAP|';var rows=t.querySelectorAll('tr'),days=[[],[],[],[],[],[],[]];for(var r=1;r<rows.length;r++){var c=rows[r].querySelectorAll('th,td');var slot=c.length?(c[0].innerText||'').trim():'';for(var d=1;d<=7;d++){if(c.length<=d)continue;var val=(c[d].innerText||'').trim();if(val&&val!=='-'&&val!=='无'){val=val.replace(/\\s+/g,' ').trim();days[d-1].push((slot?slot+'  ':'')+val);}}}return 'JWCAP7|'+days.map(function(a){return encodeURIComponent(a.join('\\n'));}).join('|');})()":type==1?"(function(){return 'BILLCAP|'+(document.body?document.body.innerText:'').slice(0,12000);})()":"(function(){return 'CARDCAP|'+(document.body?document.body.innerText:'').slice(0,12000);})()";
                    v.evaluateJavascript(js,x->{
                        if(finished[0])return;
                        boolean ok=isRefreshPayloadValid(x,type);
                        if(ok)parseSync(x,type==0,type==1,type==2);
                        finished[0]=true;try{v.stopLoading();v.destroy();}catch(Exception ignored){}
                        finishBackgroundRefresh(ok);
                    });
                },type==1?1800:900);
            }
        });
        w.loadUrl(url);new android.os.Handler().postDelayed(timeout,10000);
    }
    private boolean isRefreshPayloadValid(String raw,int type){
        String x=jsClean(raw).replaceAll("\\s+"," ").trim();
        if(type==0){int p=x.indexOf("JWCAP7|");if(p>=0)return x.substring(p+7).trim().length()>0;p=x.indexOf("JWCAP|");return p>=0&&x.substring(p+6).trim().length()>0;}
        if(type==1)return extractNumber(x,new String[]{"剩余电费","剩余金额","账户余额","余额","overplus"}).length()>0;
        return extractNumber(x,new String[]{"账户余额","卡余额","可用余额","余额"}).length()>0;
    }
    private void finishBackgroundRefresh(boolean success){
        if(!backgroundRefreshing)return;
        if(!success)backgroundRefreshFailures++;
        if(backgroundRefreshPending>0)backgroundRefreshPending--;
        if(backgroundRefreshPending==0){
            backgroundRefreshing=false;
            if(homeRefresh!=null)homeRefresh.setRefreshing(false);
            if(backgroundRefreshFailures>0)Toast.makeText(this,"首页刷新失败：有 "+backgroundRefreshFailures+" 项未能更新，请检查网络或登录状态",Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout accountCard(String label,String value,String hint,String url){LinearLayout c=card();c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->open(label,url));c.addView(t(label,15,Color.WHITE));TextView val=title(value,24);val.setPadding(dp(4),dp(12),dp(4),0);c.addView(val);c.addView(t(hint,13,Color.rgb(85,157,255)));return c;}
    private Button navButton(String s,boolean active){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(active?BLUE:MUTED);b.setBackgroundColor(Color.TRANSPARENT);return b;}

    @SuppressLint("SetJavaScriptEnabled") private void ensureBrowser(){if(browserView!=null)return;browserView=new LinearLayout(this);browserView.setOrientation(LinearLayout.VERTICAL);browserView.setBackgroundColor(BG);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(4),dp(4),dp(4),dp(4));bar.setBackgroundColor(Color.rgb(18,39,66));
        Button back=navButton("‹",true),home=navButton("首页",true),refresh=navButton("刷新",false);browserTitle=title("",15);browserTitle.setGravity(Gravity.CENTER);
        bar.addView(back,new LinearLayout.LayoutParams(dp(54),dp(54)));bar.addView(home,new LinearLayout.LayoutParams(dp(72),dp(54)));bar.addView(browserTitle,new LinearLayout.LayoutParams(0,dp(54),1));bar.addView(refresh,new LinearLayout.LayoutParams(dp(72),dp(54)));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);webView=new WebView(this);WebSettings s=webView.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);
        webView.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();String host=u.getHost();if(host==null)return true;boolean ok=allowed.contains(host)||host.endsWith(".ecjtu.edu.cn");return !ok;}@Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();}@Override public void onPageFinished(WebView v,String u){progress.setVisibility(View.GONE);CookieManager.getInstance().flush();captureSchedule(v,u);syncFromPage(v,u);if(setupInProgress&&u!=null&&u.contains("jwxt.ecjtu.edu.cn/index.action")){browserTitle.setText("首次设置 · 正在配置");new android.os.Handler().postDelayed(()->webView.loadUrl(BILLSYS_OAUTH),500);return;}if(setupInProgress&&u!=null&&u.contains("billsys.ecjtu.edu.cn/student/")){setupInProgress=false;prefs.edit().putBoolean("setupComplete",true).putBoolean("billsysLogged",true).apply();Toast.makeText(MainActivity.this,"设置完成",Toast.LENGTH_SHORT).show();new android.os.Handler().postDelayed(()->showHome(),700);return;}boolean loggedPage=u!=null&&(u.contains("jwxt.ecjtu.edu.cn/index.action")||u.contains("ecard.ecjtu.edu.cn/"));if(loggedPage&&returnAfterLogin){returnAfterLogin=false;new android.os.Handler().postDelayed(()->showHome(),900);}}});
        webView.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView v,int p){progress.setVisibility(p<100?View.VISIBLE:View.GONE);progress.setProgress(p);}});
        back.setOnClickListener(v->{if(webView.canGoBack())webView.goBack();else showHome();});home.setOnClickListener(v->showHome());refresh.setOnClickListener(v->webView.reload());
        browserView.addView(bar);browserView.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));browserView.addView(webView,new LinearLayout.LayoutParams(-1,0,1));}
    private void open(String title,String url){open(title,url,true);}
    private void open(String title,String url,boolean returnHomeAfterLogin){ensureBrowser();browserTitle.setText(title);setContentView(browserView);browserView.setVisibility(View.VISIBLE);progress.setVisibility(View.VISIBLE);returnAfterLogin=returnHomeAfterLogin&&!setupInProgress&&url.contains("mobile.ecjtu.edu.cn/oauth/authorize")&&!url.equals(BILLSYS_OAUTH);webView.loadUrl(url);}
    private void showHome(){if(!prefs.getBoolean("setupComplete",false)){buildSetup();return;}buildHome();}
    private void captureSchedule(WebView v,String url){
        if(url==null||!url.contains("jwxt.ecjtu.edu.cn"))return;
        if(url.contains("Schedule_getUserSchedume.action")){
            v.evaluateJavascript("(function(){var e=document.getElementById('courseSche');return e?e.outerHTML:'';})()", value->{ if(value!=null&&value.length()>4) lastJwxtHtml=value; });
        }
    }
    private void syncBillsysAndReturn(WebView v){
        v.evaluateJavascript("(function(){return 'BILLCAP|'+(document.body?document.body.innerText:'').slice(0,12000);})()",raw->{
            String x=jsClean(raw).replaceAll("\\s+"," ").trim();String val=extractNumber(x,new String[]{"剩余电费","剩余金额","账户余额","余额","overplus"});
            if(val.length()>0){if(!val.startsWith("¥")&&!val.startsWith("￥"))val="¥"+val;saveValue("power",val);prefs.edit().putBoolean("billsysLogged",true).apply();Toast.makeText(this,"宿舍剩余电费已更新",Toast.LENGTH_SHORT).show();showHome();}
        });
    }
    private void syncFromPage(WebView v,String url){
        if(url==null)return;
        boolean jw=url.contains("jwxt.ecjtu.edu.cn"), bill=url.contains("billsys.ecjtu.edu.cn")||url.contains("atrustweb.ecjtu.edu.cn"), card=url.contains("ecard.ecjtu.edu.cn");
        if(!jw&&!bill&&!card)return;
        String js;
        if(jw) js="(function(){var t=document.getElementById('courseSche');if(!t)return 'JWCAP|';var rows=t.querySelectorAll('tr'),days=[[],[],[],[],[],[],[]];for(var r=1;r<rows.length;r++){var c=rows[r].querySelectorAll('th,td');var slot=c.length?(c[0].innerText||'').trim():'';for(var d=1;d<=7;d++){if(c.length<=d)continue;var val=(c[d].innerText||'').trim();if(val&&val!=='-'&&val!=='无'){val=val.replace(/\\s+/g,' ').trim();days[d-1].push((slot?slot+'  ':'')+val);}}}return 'JWCAP7|'+days.map(function(a){return encodeURIComponent(a.join('\\n'));}).join('|');})()";
        else if(bill) js="(function(){return 'BILLCAP|'+(document.body?document.body.innerText:'').slice(0,5000);})()";
        else js="(function(){return 'CARDCAP|'+(document.body?document.body.innerText:'').slice(0,5000);})()";
        v.evaluateJavascript(js,value->{parseSync(value,jw,bill,card);});
    }
    private String jsClean(String s){if(s==null)return "";if(s.length()>=2&&s.charAt(0)=='"'&&s.charAt(s.length()-1)=='"')s=s.substring(1,s.length()-1);return s.replace("\\n"," ").replace("\\\"","\"");}
    private void parseSync(String raw,boolean jw,boolean bill,boolean card){
        String cleaned=jsClean(raw);
        if(jw){
            int p7=cleaned.indexOf("JWCAP7|");
            if(p7>=0){
                String payload=cleaned.substring(p7+7);String[] encoded=payload.split("\\|",-1);
                if(encoded.length>=7){
                    String[] weekdayCourses=new String[7];
                    for(int i=0;i<7;i++){
                        try{weekdayCourses[i]=java.net.URLDecoder.decode(encoded[i],"UTF-8").trim();}catch(Exception ex){weekdayCourses[i]="";}
                        if(!weekdayCourses[i].isEmpty()){
                            String rawDay=weekdayCourses[i].replace("\\r","");
                            rawDay=rawDay.replaceAll("(?<!^)\\s+(?=(?:1-2|3-4|5-6|7-8|9-10|11-12|13-14)\\s)","\\n");
                            String[] lines=rawDay.split("\\n");StringBuilder nice=new StringBuilder();
                            for(String line:lines){line=line.trim();if(line.isEmpty())continue;if(nice.length()>0)nice.append("\n");nice.append("• ").append(line);}
                            weekdayCourses[i]=nice.toString();
                        }
                    }
                    String[] days=new String[31];Calendar today=Calendar.getInstance();
                    for(int i=0;i<31;i++){
                        Calendar d=(Calendar)today.clone();d.add(Calendar.DAY_OF_MONTH,i-15);
                        String source=weekdayCourses[dayOfWeekIndex(d)];int weekNo=getSchoolWeek(d);StringBuilder visible=new StringBuilder();
                        if(!source.isEmpty()){for(String line:source.split("\\n")){line=line.trim();if(line.isEmpty()||!courseOccursInWeek(line,weekNo))continue;if(visible.length()>0)visible.append("\\n");visible.append(line);}}
                        days[i]=visible.length()==0?(i==15?"今日无事":"暂无课程"):visible.toString();
                    }
                    scheduleDays=days;StringBuilder packed=new StringBuilder();for(String day:days){if(packed.length()>0)packed.append((char)30);packed.append(day);}
                    prefs.edit().putString("scheduleDays",packed.toString()).putBoolean("jwxtLogged",true).apply();
                    selectedScheduleIndex=15;todaySummary=days[15];saveValue("schedule",todaySummary);
                    if(scheduleValue!=null){renderScheduleDayStrip();updateScheduleDayDisplay();}
                }
            }else{
                int p=cleaned.indexOf("JWCAP|");String table=p>=0?cleaned.substring(p+6).trim():"";
                table=table.replace("\\\\n","\\n").replace("\\n","\\n").replaceAll("[ \\t]+"," ").trim();
                if(p>=0){if(table.isEmpty()||"EMPTY".equalsIgnoreCase(table))table="今日无事";else{String[] lines=table.split("\n");StringBuilder nice=new StringBuilder();for(String line:lines){line=line.trim();if(line.isEmpty())continue;if(nice.length()>0)nice.append("\n");nice.append("• ").append(line);}table=nice.toString();if(table.length()>320)table=table.substring(0,320)+"…";}todaySummary=table;saveValue("schedule",table);if(scheduleValue!=null)scheduleValue.setText(table);prefs.edit().putBoolean("jwxtLogged",true).apply();}
            }
        }else if(bill){
            String x=cleaned.replaceAll("\\s+"," ").trim();String val=extractNumber(x,new String[]{"剩余电费","剩余金额","账户余额","余额","overplus"});
            if(val.length()>0){saveValue("power",val);if(powerValue!=null)powerValue.setText(val);prefs.edit().putBoolean("billsysLogged",true).apply();}
        }else if(card){
            String x=cleaned.replaceAll("\\s+"," ").trim();String bal=extractNumber(x,new String[]{"账户余额","卡余额","可用余额","余额"});String spend=extractNumber(x,new String[]{"本月支出","消费金额"});
            if(bal.length()>0){saveValue("card",bal);if(cardValue!=null)cardValue.setText(bal);prefs.edit().putBoolean("ecardLogged",true).apply();}
            if(spend.length()>0){saveValue("spend",spend);if(spendValue!=null)spendValue.setText(spend);}
        }
    }
    private String extractNumber(String s,String[] keys){for(String k:keys){int p=s.indexOf(k);if(p>=0){String tail=s.substring(p,Math.min(s.length(),p+80));java.util.regex.Matcher m=java.util.regex.Pattern.compile("[-+]?\\d+(?:\\.\\d+)?").matcher(tail);if(m.find())return m.group();}}return "";}
    private void saveValue(String key,String value){if(value!=null&&!value.trim().isEmpty())prefs.edit().putString(key,value.trim()).apply();}
    @Override protected void onDestroy(){if(webView!=null){webView.stopLoading();webView.destroy();}super.onDestroy();}
}
