package cn.edu.ecjtu;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Looper;
import org.json.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.os.Environment;
import android.os.Build;
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
    private static final int BG=Color.rgb(9,18,34), CARD=Color.rgb(25,43,66), BLUE=Color.rgb(91,166,255), MUTED=Color.rgb(164,184,208);
    private LinearLayout homeView,browserView,widgetEditBar; private GridLayout widgetContainer; private WebView webView; private SwipeRefreshLayout homeRefresh; private ScrollView homeScrollView,scheduleScrollView; private boolean scheduleTouchInProgress=false; private int backgroundRefreshPending=0; private boolean backgroundRefreshing=false; private int backgroundRefreshFailures=0; private boolean setupInProgress=false; private TextView browserTitle; private ProgressBar progress; private TextView scheduleValue,scheduleDateLabel,powerValue,cardValue,spendValue,hitokotoValue,hitokotoFrom; private LinearLayout scheduleDayStrip,scheduleTimeline; private String[] scheduleDays=new String[31]; private int selectedScheduleIndex=15; private HorizontalScrollView scheduleDayScroll; private android.content.SharedPreferences prefs; private static final int REQ_RESTORE_SESSION=4102; private String pendingBackupPassword=null; private String lastJwxtRawData=""; private String lastJwxtParsedData=""; private String lastJwxtCaptureStatus="尚未抓取"; private String lastJwxtCaptureAt=""; private String todaySummary="尚未同步课表"; private boolean returnAfterLogin=false; private boolean widgetEditing=false; private boolean renderingDashboardWidgets=false; private TextView weatherWidgetCity,weatherWidgetInfo; private LinearLayout weatherPage; private JSONObject weatherData; private static final int REQ_WEATHER_LOCATION=7301; private WeatherLocationClient weatherLocationClient;
    private static final String JWXT_OAUTH="https://mobile.ecjtu.edu.cn/oauth/authorize?client_id=pqZ3wGM07i8R9mR3&response_type=code&scope=base_api&state=ecjtu&redirect_uri=https%3A%2F%2Fmobileapp.ecjtu.edu.cn%2Fcheck%2Fredirectapp%3Fredirect_url%3Dhttps%253A%252F%252Fjwxt.ecjtu.edu.cn%252FstuMag%252FLogin_dcpLogin.action";
    private static final String BILLSYS_OAUTH="https://mobile.ecjtu.edu.cn/oauth/authorize?client_id=8fa4065773ff0df4&redirect_uri=http%3A%2F%2Fbillsys.ecjtu.edu.cn%2Fstudent%2F%23%2Fpages%2Fbindinfo%2Fbindinfo&response_type=code&scope=base_api&state=zhjdapp";
    private final Set<String> allowed=new HashSet<>(Arrays.asList("portal.ecjtu.edu.cn","cas.ecjtu.edu.cn","mobile.ecjtu.edu.cn","mobileapp.ecjtu.edu.cn","jwxt.ecjtu.edu.cn","ecard.ecjtu.edu.cn","billsys.ecjtu.edu.cn","atrustweb.ecjtu.edu.cn","xymjxt.ecjtu.edu.cn","zhxg.ecjtu.edu.cn","zhxgapi.ecjtu.edu.cn"));

    @Override public void onCreate(Bundle b){super.onCreate(b);configureEdgeToEdge();CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().flush();prefs=getSharedPreferences("local_session",MODE_PRIVATE);DiagnosticLogger.cleanupDiagnosticLogs(prefs);if(prefs.getBoolean("setupComplete",false))buildHome();else buildSetup();getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){public void handleOnBackPressed(){if(browserView!=null&&browserView.getVisibility()==View.VISIBLE){if(webView.canGoBack())webView.goBack();else showHome();}else finish();}});}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    // Glassmorphism built from translucent gradients and fine highlights; avoids API-specific backdrop blur.
    private GradientDrawable glass(int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(224,42,66,96),Color.argb(196,24,43,68),Color.argb(214,17,33,55)});
        d.setCornerRadius(dp(radius));d.setStroke(dp(1),Color.argb(82,211,232,255));return d;
    }
    private GradientDrawable pageBackground(){
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(15,31,52),Color.rgb(8,17,32),Color.rgb(13,26,45)});
    }
    private TextView t(String s,float z,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(color);v.setPadding(dp(4),dp(5),dp(4),dp(5));return v;}
    private TextView title(String s,float z){TextView v=t(s,z,Color.WHITE);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private void addSpace(LinearLayout p,int h){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    private void applyPagePadding(View root,int left,int top,int right,int bottom){root.setPadding(left,top,right,bottom);}
    private void configureEdgeToEdge(){
        android.view.Window window=getWindow();
        WindowCompat.setDecorFitsSystemWindows(window,false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=29){
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        if(Build.VERSION.SDK_INT>=28)window.setNavigationBarDividerColor(Color.TRANSPARENT);
        androidx.core.view.WindowInsetsControllerCompat controller=
                WindowCompat.getInsetsController(window,window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    private void applySystemBarInsets(View root){
        final int baseLeft=root.getPaddingLeft(),baseTop=root.getPaddingTop(),
                baseRight=root.getPaddingRight(),baseBottom=root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            androidx.core.graphics.Insets safeInsets=insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            view.setPadding(baseLeft+safeInsets.left,baseTop+safeInsets.top,
                    baseRight+safeInsets.right,baseBottom+safeInsets.bottom);
            return insets;
        });
        // Request after attachment so Android 15+ dispatches the initial insets reliably.
        root.post(()->root.requestApplyInsets());
    }

    private void buildSetup(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setBackground(pageBackground());applyPagePadding(root,dp(28),dp(30),dp(28),dp(30));
        TextView mark=title("ECJTUEE",34);mark.setGravity(Gravity.CENTER);root.addView(mark,new LinearLayout.LayoutParams(-1,-2));
        TextView hello=title("欢迎使用",28);hello.setGravity(Gravity.CENTER);hello.setPadding(0,dp(36),0,dp(8));root.addView(hello);
        TextView intro=t("像设置一台新设备一样，只需完成一次校园账号登录。登录状态将由 Android WebView 保存在本机，ECJTUEE 不会保存你的账号密码，也不会上传登录信息。",15,MUTED);intro.setGravity(Gravity.CENTER);root.addView(intro,new LinearLayout.LayoutParams(-1,-2));
        Space flex=new Space(this);root.addView(flex,new LinearLayout.LayoutParams(1,0,1));
        LinearLayout info=card();info.addView(title("设置校园服务",19));info.addView(t("登录后将用于课表、宿舍电费、校园卡等校内服务。后续可直接使用已有的本机登录状态。",14,MUTED));root.addView(info,new LinearLayout.LayoutParams(-1,-2));addSpace(root,18);
        LinearLayout semester=card();semester.addView(title("开学日期设定",18));semester.addView(t("设置本学期第一周周一的日期，首页将自动显示当前周次。",13,MUTED));Button semesterBtn=new Button(this);semesterBtn.setText(prefs.getString("semesterStart","").length()>0?"已设置："+prefs.getString("semesterStart",""):"设置开学日期");semesterBtn.setAllCaps(false);semesterBtn.setTextColor(Color.WHITE);semesterBtn.setBackground(glass(14));semesterBtn.setOnClickListener(v->chooseSemesterStart());semester.addView(semesterBtn,new LinearLayout.LayoutParams(-1,dp(50)));root.addView(semester);addSpace(root,18);
        Button login=new Button(this);login.setText("登录校园账号");login.setAllCaps(false);login.setTextColor(Color.WHITE);login.setTextSize(16);login.setBackground(bg(Color.rgb(55,132,230),18));login.setElevation(dp(4));login.setOnClickListener(v->{setupInProgress=true;open("首次设置 · 登录",JWXT_OAUTH);});root.addView(login,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView note=t("继续即表示你将前往学校官方统一身份认证页面。",12,MUTED);note.setGravity(Gravity.CENTER);note.setPadding(0,dp(14),0,0);root.addView(note);
        applySystemBarInsets(root);setContentView(root);
    }
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(glass(22));c.setElevation(dp(2));return c;}
    private Button action(String label,String url){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setBackground(glass(16));b.setElevation(dp(2));b.setOnClickListener(v->open(label,url));return b;}

    private void buildHome(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackground(pageBackground());applyPagePadding(root,0,dp(30),0,0);
        ScrollView scroll=new ScrollView(this);homeScrollView=scroll;homeView=new LinearLayout(this);homeView.setOrientation(LinearLayout.VERTICAL);homeView.setPadding(dp(20),dp(18),dp(20),dp(32));scroll.addView(homeView); homeRefresh=new SwipeRefreshLayout(this); homeRefresh.addView(scroll); homeRefresh.setOnRefreshListener(this::refreshAllHomeInBackground); homeRefresh.setOnChildScrollUpCallback((parent,child)->{ boolean outerCanScrollUp=homeScrollView!=null&&homeScrollView.canScrollVertically(-1); boolean innerScheduleCanScrollUp=scheduleTouchInProgress&&scheduleScrollView!=null&&scheduleScrollView.canScrollVertically(-1); return outerCanScrollUp||innerScheduleCanScrollUp; });

        Calendar cal=Calendar.getInstance();int h=cal.get(Calendar.HOUR_OF_DAY);String greet=h<6?"夜深了":h<11?"早上好":h<14?"中午好":h<18?"下午好":"晚上好";int week=getSchoolWeek(cal);String weekText=week>0?"第"+week+"周":"未设置周次";
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.HORIZONTAL);hero.setGravity(Gravity.BOTTOM);
        TextView greetView=title(greet+"，",31);hero.addView(greetView,new LinearLayout.LayoutParams(0,-2,1));
        TextView weekView=t(weekText,15,MUTED);weekView.setGravity(Gravity.RIGHT|Gravity.BOTTOM);weekView.setPadding(dp(4),dp(5),0,dp(8));hero.addView(weekView,new LinearLayout.LayoutParams(-2,-2));homeView.addView(hero);
        hitokotoValue=t(prefs.getString("hitokoto","正在获取一言…"),15,MUTED);hitokotoValue.setPadding(dp(4),dp(5),dp(4),0);homeView.addView(hitokotoValue);
        String cachedFrom=prefs.getString("hitokoto_from","");boolean hasFrom=cachedFrom!=null&&!cachedFrom.trim().isEmpty()&&!cachedFrom.trim().equalsIgnoreCase("null")&&!cachedFrom.trim().equals("—— null");hitokotoFrom=t(hasFrom?cachedFrom:"",12,MUTED);hitokotoFrom.setPadding(dp(4),dp(2),dp(4),dp(5));hitokotoFrom.setVisibility(hasFrom?View.VISIBLE:View.GONE);homeView.addView(hitokotoFrom);
        fetchHitokotoIfNeeded();
        addSpace(homeView,18);




        widgetEditBar=new LinearLayout(this);widgetEditBar.setGravity(Gravity.CENTER_VERTICAL);widgetEditBar.setVisibility(View.GONE);
        TextView editHint=t("长按卡片进入编辑，可拖动排序",12,MUTED);widgetEditBar.addView(editHint,new LinearLayout.LayoutParams(0,-2,1));
        Button addWidget=navButton("＋ 添加",true);addWidget.setOnClickListener(v->showAddWidgetDialog());widgetEditBar.addView(addWidget,new LinearLayout.LayoutParams(dp(96),dp(44)));
        Button doneWidget=navButton("完成",true);doneWidget.setOnClickListener(v->setWidgetEditing(false));widgetEditBar.addView(doneWidget,new LinearLayout.LayoutParams(dp(76),dp(44)));
        homeView.addView(widgetEditBar);widgetContainer=new GridLayout(this);widgetContainer.setColumnCount(4);homeView.addView(widgetContainer);renderDashboardWidgets();
                homeView.addView(t("\n登录信息仅保存在本机 WebView Cookie 和应用本地偏好中，不上传到第三方。",12,MUTED));

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(12),dp(6),dp(12),dp(8));nav.setBackground(glass(24));nav.setElevation(dp(8));nav.setClipToOutline(true);
        Button n1=navButton("⌂\n首页",true),n2=navButton("▦\n功能",false),n3=navButton("⚙\n设置",false);
        n1.setOnClickListener(v->{});n2.setOnClickListener(v->open("智慧教务",JWXT_OAUTH));n3.setOnClickListener(v->buildSettings());
        nav.addView(n1,new LinearLayout.LayoutParams(0,dp(72),1));nav.addView(n2,new LinearLayout.LayoutParams(0,dp(72),1));nav.addView(n3,new LinearLayout.LayoutParams(0,dp(72),1));
        root.addView(homeRefresh,new LinearLayout.LayoutParams(-1,0,1));root.addView(nav);applySystemBarInsets(root);setContentView(root);
        homeRefresh.post(()->{if(!backgroundRefreshing){homeRefresh.setRefreshing(true);refreshAllHomeInBackground();}});
    }


    private ArrayList<String> getWidgetOrder(){
        String saved=prefs.getString("home_widget_order","");
        ArrayList<String> order=new ArrayList<>();
        if(saved.trim().isEmpty()||"schedule,power,card,spend".equals(saved)){order.add("schedule");order.add("power");order.add("weather");saveWidgetOrder(order);return order;}
        for(String id:saved.split(","))if(Arrays.asList("schedule","power","weather","card","spend").contains(id)&&!order.contains(id))order.add(id);
        return order;
    }
    private void saveWidgetOrder(ArrayList<String> order){prefs.edit().putString("home_widget_order",android.text.TextUtils.join(",",order)).apply();}
    private void setWidgetEditing(boolean editing){
        boolean changed=widgetEditing!=editing;
        widgetEditing=editing;
        if(widgetEditBar!=null)widgetEditBar.setVisibility(editing?View.VISIBLE:View.GONE);
        if(changed&&widgetContainer!=null){
            // Rebuild only after the editing state changes so the insertion drop target exists
            // before a drag starts. The long-press handler then resolves the refreshed source view.
            renderDashboardWidgets();
        }
        if(widgetContainer!=null)for(int i=0;i<widgetContainer.getChildCount();i++){
            View v=widgetContainer.getChildAt(i);
            View remove=v.findViewWithTag("widget_remove");
            if(remove!=null)remove.setVisibility(editing?View.VISIBLE:View.GONE);
            if("widget_drop_target".equals(v.getTag()))v.setVisibility(editing?View.VISIBLE:View.GONE);
            v.setAlpha(editing?0.94f:1f);
        }
    }
    private void showAddWidgetDialog(){
        ArrayList<String> order=getWidgetOrder(),missing=new ArrayList<>(),labels=new ArrayList<>();
        String[] ids={"schedule","power","weather","card","spend"},names={"课表","宿舍剩余电费","天气","校园卡余额","本月支出"};
        for(int i=0;i<ids.length;i++)if(!order.contains(ids[i])){missing.add(ids[i]);labels.add(names[i]);}
        if(missing.isEmpty()){Toast.makeText(this,"所有小组件都已添加",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("添加小组件").setItems(labels.toArray(new String[0]),(d,w)->{order.add(missing.get(w));saveWidgetOrder(order);renderDashboardWidgets();setWidgetEditing(true);}).setNegativeButton("取消",null).show();
    }
    private void renderDashboardWidgets(){
        if(widgetContainer==null||renderingDashboardWidgets)return;
        renderingDashboardWidgets=true;
        try{
            widgetContainer.removeAllViews();
            widgetContainer.setColumnCount(4);
            widgetContainer.setUseDefaultMargins(false);
            widgetContainer.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
            ArrayList<String> order=getWidgetOrder();
            boolean[][] occupied=new boolean[64][4];
            int maxRow=8;
            for(String id:order){
                boolean fullWidth="schedule".equals(id);
                int columnSpan=fullWidth?4:2;
                android.widget.FrameLayout frame=createWidgetFrame(id);
                int rowSpan=fullWidth?getScheduleWidgetRows():2;
                int row=-1,col=-1;
                search:
                for(int r=0;r<64;r++){
                    for(int x=0;x<=4-columnSpan;x+=2){
                        boolean fits=true;
                        for(int yy=r;yy<r+rowSpan&&yy<64&&fits;yy++)
                            for(int xx=x;xx<x+columnSpan;xx++)if(occupied[yy][xx]){fits=false;break;}
                        if(fits){row=r;col=x;break search;}
                    }
                }
                if(row<0)continue;
                for(int yy=row;yy<row+rowSpan&&yy<64;yy++)
                    for(int xx=col;xx<col+columnSpan;xx++)occupied[yy][xx]=true;
                maxRow=Math.max(maxRow,row+rowSpan);
                GridLayout.LayoutParams lp=new GridLayout.LayoutParams(
                        GridLayout.spec(row,rowSpan),
                        GridLayout.spec(col,columnSpan,1f));
                lp.width=0;lp.height=0;
                lp.setMargins(dp(4),dp(4),dp(4),dp(12));
                frame.setLayoutParams(lp);
                widgetContainer.addView(frame);
                frame.post(()->{
                    android.view.ViewGroup.LayoutParams measured=frame.getLayoutParams();
                    if(measured instanceof GridLayout.LayoutParams){
                        GridLayout.LayoutParams sized=(GridLayout.LayoutParams)measured;
                        int width=frame.getWidth();
                        int targetHeight=fullWidth?(width*rowSpan)/4:width;
                        if(width>0&&sized.height!=targetHeight){sized.height=targetHeight;frame.setLayoutParams(sized);}
                    }
                });
            }
            // Keep an eight-row base grid; allow extra rows only when all widgets cannot fit.
            widgetContainer.setRowCount(Math.max(8,maxRow));
            int dropRow=-1,dropCol=-1;
            searchDrop:
            for(int r=0;r<64;r++)for(int x=0;x<=2;x+=2){
                boolean fits=true;
                for(int yy=r;yy<r+2&&fits;yy++)for(int xx=x;xx<x+2;xx++)if(occupied[yy][xx]){fits=false;break;}
                if(fits){dropRow=r;dropCol=x;break searchDrop;}
            }
            if(widgetEditing&&dropRow>=0){
                View target=createWidgetDropTarget(order.size());
                GridLayout.LayoutParams lp=new GridLayout.LayoutParams(GridLayout.spec(dropRow,2),GridLayout.spec(dropCol,2,1f));
                lp.width=0;lp.height=0;lp.setMargins(dp(4),dp(4),dp(4),dp(12));
                target.setLayoutParams(lp);widgetContainer.addView(target);
                target.post(()->{int side=target.getWidth();if(side>0&&target.getLayoutParams() instanceof GridLayout.LayoutParams){GridLayout.LayoutParams p=(GridLayout.LayoutParams)target.getLayoutParams();if(p.height!=side){p.height=side;target.setLayoutParams(p);}}});
                widgetContainer.setRowCount(Math.max(8,dropRow+2));
            }
            if(order.isEmpty()){
                TextView empty=t("还没有小组件，点击“添加”开始布置首页。",14,MUTED);
                empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(28),0,dp(28));
                GridLayout.LayoutParams lp=new GridLayout.LayoutParams(GridLayout.spec(0,2),GridLayout.spec(0,4,1f));
                lp.width=0;lp.height=dp(100);widgetContainer.addView(empty,lp);
            }
        }finally{renderingDashboardWidgets=false;}
    }
    private int getScheduleWidgetRows(){
        boolean multiDay=prefs!=null&&prefs.getBoolean("showMultiDaySchedule",true);
        String value;
        if(multiDay){
            value=scheduleDays!=null&&selectedScheduleIndex>=0&&selectedScheduleIndex<scheduleDays.length?scheduleDays[selectedScheduleIndex]:"";
            if(value==null||value.trim().isEmpty())value=selectedScheduleIndex==15?prefs.getString("schedule",""):"";
            int courses=splitCourseEntries(value).length;
            if(courses<=0)return 3;
            if(courses==1)return 4;
            return 5;
        }
        value=prefs==null?"":prefs.getString("schedule","尚未同步课表");
        int courses=splitCourseEntries(value).length;
        if(courses<=0)return 1;
        // Measure the rendered course cards rather than assuming one course equals one row.
        // Five rows is the fixed maximum; overflow remains available through the inner ScrollView.
        int contentWidth=0;
        if(widgetContainer!=null){
            int width=widgetContainer.getWidth();
            if(width<=0)width=getResources().getDisplayMetrics().widthPixels-dp(40);
            contentWidth=Math.max(1,width-dp(16));
        }
        int cardWidth=Math.max(dp(180),contentWidth);
        int estimatedHeight=dp(76); // header and vertical padding
        for(String entry:splitCourseEntries(value)){
            String line=entry.replaceFirst("^\\\\s*[•·]\\\\s*","").trim();
            String period="";
            java.util.regex.Matcher pm=java.util.regex.Pattern.compile("^(1-2|3-4|5-6|7-8|9-10|11-12|13-14)\\\\s+").matcher(line);
            if(pm.find()){period=pm.group(1);line=line.substring(pm.end()).trim();}
            line=line.replaceFirst("\\\\s+\\\\d+(?:-\\\\d+)?(?:\\\\s*,\\\\s*\\\\d+(?:-\\\\d+)?)*(?:\\\\s*[（(](?:单|双)[）)])?\\\\s+[\\\\d,]+$","").trim();
            int charsPerLine=Math.max(8,(cardWidth-dp(112))/dp(8));
            int lines=Math.max(1,(int)Math.ceil((double)line.length()/charsPerLine));
            int cardHeight=dp(26)+lines*dp(25)+dp(9);
            estimatedHeight+=Math.max(dp(64),cardHeight)+dp(9);
        }
        int rowHeight=Math.max(dp(64),(getResources().getDisplayMetrics().widthPixels-dp(40))/4);
        return Math.max(1,Math.min(5,(int)Math.ceil((double)estimatedHeight/rowHeight)));
    }
    private View createWidgetDropTarget(int targetIndex){
        android.widget.FrameLayout target=new android.widget.FrameLayout(this);
        target.setTag("widget_drop_target");target.setVisibility(widgetEditing?View.VISIBLE:View.GONE);
        target.setBackground(bg(Color.argb(38,125,177,235),18));
        TextView plus=t("＋",28,MUTED);plus.setGravity(Gravity.CENTER);target.addView(plus,new android.widget.FrameLayout.LayoutParams(-1,-1));
        target.setOnDragListener((v,event)->{
            if(event.getAction()==android.view.DragEvent.ACTION_DROP){String from=String.valueOf(event.getLocalState());moveWidgetToIndex(from,targetIndex);return true;}
            return event.getAction()==android.view.DragEvent.ACTION_DRAG_STARTED||event.getAction()==android.view.DragEvent.ACTION_DRAG_ENTERED||event.getAction()==android.view.DragEvent.ACTION_DRAG_EXITED||event.getAction()==android.view.DragEvent.ACTION_DRAG_LOCATION;
        });
        return target;
    }
    private void moveWidgetToIndex(String from,int targetIndex){
        ArrayList<String> current=getWidgetOrder();
        int fromIndex=current.indexOf(from);
        if(fromIndex<0)return;
        String moving=current.remove(fromIndex);
        int destination=Math.max(0,Math.min(targetIndex,current.size()));
        current.add(destination,moving);
        saveWidgetOrder(current);
        renderDashboardWidgets();
        setWidgetEditing(true);
    }
    private android.widget.FrameLayout createWidgetFrame(String id){
        View content;
        if("schedule".equals(id))content=createScheduleWidget();
        else if("power".equals(id))content=createAccountWidget("⚡  宿舍剩余电费",prefs.getString("power","未同步"),"宿舍电费","https://billsys.ecjtu.edu.cn/student/#/");
        else if("weather".equals(id))content=createWeatherWidget();
        else if("card".equals(id))content=createAccountWidget("▣  校园卡余额",prefs.getString("card","未同步"),"消费 / 充值","https://ecard.ecjtu.edu.cn/");
        else content=createAccountWidget("▥  本月支出",prefs.getString("spend","未同步"),"校园卡消费汇总","https://ecard.ecjtu.edu.cn/");
        android.widget.FrameLayout frame=new android.widget.FrameLayout(this);frame.setTag(id);frame.addView(content,new android.widget.FrameLayout.LayoutParams(-1,-1));
        View.OnLongClickListener longClick=v->{
            setWidgetEditing(true);
            View source=widgetContainer==null?null:widgetContainer.findViewWithTag(id);
            if(source==null)source=frame;
            android.content.ClipData data=android.content.ClipData.newPlainText("home-widget",id);
            source.startDragAndDrop(data,new View.DragShadowBuilder(source),id,0);
            return true;
        };
        frame.setOnLongClickListener(longClick);content.setOnLongClickListener(longClick);
        if("schedule".equals(id))content.setOnClickListener(v->open("智慧教务",JWXT_OAUTH,false));
        TextView remove=title("×",20);remove.setTag("widget_remove");remove.setGravity(Gravity.CENTER);remove.setBackground(bg(Color.argb(230,205,69,79),18));
        android.widget.FrameLayout.LayoutParams rp=new android.widget.FrameLayout.LayoutParams(dp(34),dp(34),Gravity.RIGHT|Gravity.TOP);rp.setMargins(0,dp(6),dp(6),0);frame.addView(remove,rp);
        remove.setClickable(true);remove.setFocusable(true);remove.setElevation(dp(24));remove.setOnClickListener(v->{v.setEnabled(false);ArrayList<String> current=getWidgetOrder();current.remove(id);saveWidgetOrder(current);renderDashboardWidgets();setWidgetEditing(true);});remove.setVisibility(widgetEditing?View.VISIBLE:View.GONE);remove.bringToFront();remove.post(remove::bringToFront);
        frame.setOnDragListener((v,event)->{if(event.getAction()==android.view.DragEvent.ACTION_DROP){String from=String.valueOf(event.getLocalState()),to=String.valueOf(v.getTag());if(!from.equals(to))moveWidgetToIndex(from,getWidgetOrder().indexOf(to));return true;}return event.getAction()==android.view.DragEvent.ACTION_DRAG_STARTED||event.getAction()==android.view.DragEvent.ACTION_DRAG_ENTERED||event.getAction()==android.view.DragEvent.ACTION_DRAG_EXITED||event.getAction()==android.view.DragEvent.ACTION_DRAG_LOCATION;});return frame;
    }
    private LinearLayout createAccountWidget(String label,String value,String hint,String url){
        LinearLayout c=card();c.setMinimumHeight(dp(132));c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->open(label,url));c.addView(t(label,15,Color.WHITE));TextView val=title(value,24);val.setPadding(dp(4),dp(12),dp(4),0);c.addView(val);if(label.contains("宿舍"))powerValue=val;else if(label.contains("校园卡"))cardValue=val;else if(label.contains("本月"))spendValue=val;c.addView(t(hint,13,Color.rgb(85,157,255)));return c;
    }


    private LinearLayout createWeatherWidget(){
        LinearLayout c=card();c.setMinimumHeight(dp(132));c.setOnClickListener(v->openWeather());
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=title("☀  今日天气",15);row.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        weatherWidgetCity=t(prefs.getString("weather_city","首次点击获取定位天气"),13,MUTED);row.addView(weatherWidgetCity);c.addView(row);
        LinearLayout data=new LinearLayout(this);data.setGravity(Gravity.CENTER_VERTICAL);
        weatherWidgetInfo=title(prefs.getString("weather_brief","点击查看天气详情"),24);weatherWidgetInfo.setPadding(dp(4),dp(12),dp(4),0);data.addView(weatherWidgetInfo,new LinearLayout.LayoutParams(0,-2,1));
        c.addView(data);return c;
    }
    private void openWeather(){
        buildWeatherPage();
        refreshWeatherLocation();
    }
    private void buildWeatherPage(){
        weatherPage=new LinearLayout(this);weatherPage.setOrientation(LinearLayout.VERTICAL);weatherPage.setBackground(pageBackground());
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(12),dp(8),dp(12),dp(8));
        Button back=navButton("‹ 返回",true);back.setOnClickListener(v->buildHome());bar.addView(back);
        TextView title=title("天气",19);title.setGravity(Gravity.CENTER);bar.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));
        Button refresh=navButton("刷新",false);refresh.setOnClickListener(v->refreshWeatherLocation());bar.addView(refresh);weatherPage.addView(bar);
        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(8),dp(18),dp(28));scroll.addView(body);weatherPage.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));applySystemBarInsets(weatherPage);setContentView(weatherPage);
        if(weatherData==null)weatherData=WeatherCacheManager.load(prefs);
        renderWeatherPage();
    }
    private void refreshWeatherLocation(){
        if(weatherPage!=null)renderWeatherPage();
        boolean fine=checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
        boolean coarse=checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
        if(!fine&&!coarse){
            if(!prefs.getBoolean("weather_location_permission_asked",false)){
                prefs.edit().putBoolean("weather_location_permission_asked",true).apply();
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_WEATHER_LOCATION);
            }else requestWeatherLocation();
            return;
        }
        requestGpsWeatherLocation();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQ_WEATHER_LOCATION){
            boolean granted=false;for(int result:grantResults)if(result==PackageManager.PERMISSION_GRANTED)granted=true;
            DiagnosticLogger.diagnostic(prefs,"天气","位置权限结果",granted?"已授权":"未授权",
                    "权限回调数量="+grantResults.length+"；将"+(granted?"尝试设备定位":"直接使用 IP 定位"));
            if(granted)requestGpsWeatherLocation();else requestWeatherLocation();
        }
    }
    private void requestGpsWeatherLocation(){
        if(weatherLocationClient!=null)weatherLocationClient.cancel();
        weatherLocationClient=new WeatherLocationClient(this,prefs,new WeatherLocationClient.Callback(){
            @Override public void onSearching(){
                Toast.makeText(MainActivity.this,"正在获取 GPS 定位…",Toast.LENGTH_SHORT).show();
            }
            @Override public void onLocation(Location location){
                resolveCmaStation(location);
            }
            @Override public void onFallback(){
                requestWeatherLocation();
            }
        });
        weatherLocationClient.start();
    }
    private void requestWeatherLocation(){
        Toast.makeText(this,"GPS 不可用，正在根据当前 IP 定位…",Toast.LENGTH_SHORT).show();
        DiagnosticLogger.diagnostic(prefs,"天气","IP 定位回退","开始",
                "GPS 定位未取得可用结果，准备调用 ipwho.is 免费 IP 地理位置接口");
        new Thread(()->{try{
            DiagnosticLogger.diagnostic(prefs,"天气","IP 城市定位","开始","准备请求 IP 地理位置接口");
            WeatherLocationManager.IpCity location=WeatherLocationManager.resolveIpCity(
                    address->weatherHttpGet(address));
            DiagnosticLogger.diagnostic(prefs,"天气","IP 城市定位","解析成功",
                    "省份="+location.province+"；城市="+location.city);
            resolveCmaStationByName(location.province,location.city);
        }catch(Exception e){
            String detail=e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage());
            DiagnosticLogger.diagnostic(prefs,"天气","IP 城市定位","失败",detail);
            runOnUiThread(()->useDefaultBeijingWeather("IP 定位失败，已为你切换到北京天气。"));
        }}).start();
    }
    private void useDefaultBeijingWeather(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
        new Thread(()->{try{
            WeatherLocationManager.Station station=WeatherLocationManager.findDefaultBeijing(
                    address->weatherHttpGet(address));
            final String stationId=station.id;
            prefs.edit().putString("weather_station",stationId).putString("weather_city","北京").putBoolean("weather_ready",true).putBoolean("weather_default_beijing",true).apply();
            runOnUiThread(()->fetchCmaWeather(stationId));
        }catch(Exception e){DiagnosticLogger.diagnostic(prefs,"天气","北京默认站点","失败",e.getMessage());runOnUiThread(()->Toast.makeText(this,"北京天气也暂时无法获取，请检查网络",Toast.LENGTH_LONG).show());}}).start();
    }
    private void resolveCmaStationByName(String province,String city){
        new Thread(()->{try{
            DiagnosticLogger.diagnostic(prefs,"天气","IP 气象站匹配","开始",
                    "尝试匹配省份="+province+"；城市="+city);
            WeatherLocationManager.Station station=WeatherLocationManager.findByName(province,city,
                    address->weatherHttpGet(address));
            String id=station.id,display=station.displayName;
            prefs.edit().putString("weather_station",id).putString("weather_city",display).putBoolean("weather_ready",true).putBoolean("weather_default_beijing",false).apply();
            runOnUiThread(()->{DiagnosticLogger.diagnostic(prefs,"天气","IP 自动匹配气象站","成功",display+"；站点编号="+id);if(weatherWidgetCity!=null)weatherWidgetCity.setText(display);fetchCmaWeather(id);});
        }catch(Exception e){DiagnosticLogger.diagnostic(prefs,"天气","IP 自动匹配气象站","失败",e.getClass().getSimpleName()+": "+e.getMessage());runOnUiThread(()->useDefaultBeijingWeather("获取定位失败，已为你切换到北京天气。"));}}).start();
    }
    private void resolveCmaStation(Location location){
        new Thread(()->{try{
            WeatherGeocoder.AdministrativeArea area=WeatherGeocoder.resolve(this,location);
            WeatherLocationManager.Station station=WeatherLocationManager.findForAddress(
                    area.province,area.city,area.district,endpoint->weatherHttpGet(endpoint));
            String id=station.id,display=station.displayName;
            prefs.edit().putString("weather_station",id).putString("weather_city",display).putBoolean("weather_ready",true).putBoolean("weather_default_beijing",false).apply();
            runOnUiThread(()->{DiagnosticLogger.diagnostic(prefs,"天气","GPS 区县优先匹配","成功",display+"；站点编号="+id);if(weatherWidgetCity!=null)weatherWidgetCity.setText(display);fetchCmaWeather(id);});
        }catch(Exception e){DiagnosticLogger.diagnostic(prefs,"天气","GPS 定位与站点匹配","失败",e.getClass().getSimpleName()+": "+e.getMessage());runOnUiThread(()->requestWeatherLocation());}}).start();
    }
    private String weatherHttpGet(String address)throws Exception{
        return WeatherManager.get(prefs,address);
    }
    private void fetchCmaWeather(String station){fetchCmaWeather(station,false);}
    private void fetchCmaWeather(String station,boolean countAsHomeRefresh){
        if(station==null||station.isEmpty()){if(countAsHomeRefresh)finishBackgroundRefresh(false);return;}
        new Thread(()->{boolean refreshOk=false;try{
            WeatherDataParser.ParsedWeather parsed=WeatherDataParser.parse(
                    weatherHttpGet("https://www.nmc.cn/rest/weather?stationid="+Uri.encode(station)),
                    prefs.getString("weather_city",""));
            JSONObject data=parsed.data;
            String city=parsed.city,description=parsed.description,temperature=parsed.temperature,brief=parsed.brief;
            weatherData=data;refreshOk=true;WeatherCacheManager.save(prefs,data,city,brief);
            DiagnosticLogger.diagnostic(prefs,"天气","中央气象台数据更新","成功","城市="+city+"；天气="+description+"；气温="+temperature);
            runOnUiThread(()->{if(weatherWidgetCity!=null)weatherWidgetCity.setText(city);if(weatherWidgetInfo!=null)weatherWidgetInfo.setText(brief);if(weatherPage!=null&&weatherPage.getVisibility()==View.VISIBLE)renderWeatherPage();});
        }catch(Exception e){DiagnosticLogger.diagnostic(prefs,"天气","中央气象台数据更新","失败",e.getClass().getSimpleName()+": "+e.getMessage());runOnUiThread(()->{if(weatherData==null)Toast.makeText(this,"天气获取失败："+e.getMessage(),Toast.LENGTH_LONG).show();else Toast.makeText(this,"天气更新失败，正在显示缓存数据",Toast.LENGTH_SHORT).show();});}finally{if(countAsHomeRefresh){boolean ok=refreshOk;runOnUiThread(()->finishBackgroundRefresh(ok));}}}).start();
    }
    private String validWeatherText(JSONObject object,String key){return WeatherDataValidator.validText(object,key);}
    private String validWeatherNumber(JSONObject object,String key,double min,double max){return WeatherDataValidator.validNumber(object,key,min,max);}
    private void renderWeatherPage(){
        if(weatherPage==null)return;ScrollView scroll=(ScrollView)weatherPage.getChildAt(1);LinearLayout body=(LinearLayout)scroll.getChildAt(0);body.removeAllViews();
        if(weatherData==null){TextView wait=title("正在连接中央气象台…",20);wait.setPadding(0,dp(45),0,dp(45));body.addView(wait);body.addView(t("首次使用需要定位并匹配附近气象站。",14,MUTED));return;}
        try{
            WeatherCurrentParser.CurrentWeather current=WeatherCurrentParser.parse(weatherData);
            String temperature=current.temperature,description=current.description,feelsLike=current.feelsLike,humidity=current.humidity;
            LinearLayout hero=card();hero.setBackground(glass(28));hero.addView(t(prefs.getString("weather_city","当地天气")+" · 实时天气",15,MUTED));
            if(temperature!=null)hero.addView(title(temperature+"°",62));
            if(description!=null)hero.addView(title(description,23));
            if(feelsLike!=null)hero.addView(t("体感温度  "+feelsLike+"°",14,MUTED));
            if(humidity!=null)hero.addView(t("相对湿度  "+humidity+"%",14,MUTED));
            if(current.publishTime!=null)hero.addView(t("观测时间  "+current.publishTime,12,MUTED));
            body.addView(hero);addSpace(body,14);
            LinearLayout details=card();details.addView(title("天气详情",18));boolean hasDetail=false;
            if(current.windDirection!=null) {details.addView(t("风向  "+current.windDirection,14,MUTED));hasDetail=true;}
            if(current.windPower!=null) {details.addView(t("风力  "+current.windPower,14,MUTED));hasDetail=true;}
            if(current.windSpeed!=null) {details.addView(t("风速  "+current.windSpeed+" m/s",14,MUTED));hasDetail=true;}
            if(current.pressure!=null) {details.addView(t("气压  "+current.pressure+" hPa",14,MUTED));hasDetail=true;}
            if(current.rainfall!=null) {details.addView(t("降水量  "+current.rainfall+" mm",14,MUTED));hasDetail=true;}
            if(hasDetail){body.addView(details);addSpace(body,14);}
            LinearLayout forecast=card();forecast.addView(title("未来天气",18));
            List<WeatherForecastParser.ForecastDay> forecastDays=WeatherForecastParser.parse(weatherData);
            int forecastCount=0;
            for(WeatherForecastParser.ForecastDay forecastDay:forecastDays){
                forecast.addView(t(forecastDay.toDisplayText(),13,MUTED));forecastCount++;
            }
            if(forecastCount>0){body.addView(forecast);addSpace(body,14);}
            String warning=current.warning;
            if(warning!=null){LinearLayout alert=card();alert.addView(title("气象预警",18));alert.addView(t(warning,14,MUTED));body.addView(alert);addSpace(body,12);}
            body.addView(t("数据来源：中央气象台（国家气象中心）",12,MUTED));
        }catch(Exception e){DiagnosticLogger.diagnostic(prefs,"天气","详情渲染","失败",e.getClass().getSimpleName()+": "+e.getMessage());body.addView(t("天气详情暂时无法显示",14,MUTED));}
    }

    private LinearLayout createScheduleWidget(){
        LinearLayout schedule=card();schedule.setTag("schedule");schedule.setPadding(dp(16),dp(16),dp(16),dp(16));schedule.setBackground(glass(26));schedule.setElevation(dp(3));
        LinearLayout sh=new LinearLayout(this);sh.setGravity(Gravity.CENTER_VERTICAL);scheduleDateLabel=title("",19);sh.addView(scheduleDateLabel,new LinearLayout.LayoutParams(0,-2,1));Button all=action("查看课表",JWXT_OAUTH);all.setTextSize(13);all.setBackground(bg(Color.argb(190,54,126,218),15));all.setOnClickListener(v->open("智慧教务",JWXT_OAUTH,false));sh.addView(all,new LinearLayout.LayoutParams(dp(94),dp(42)));schedule.addView(sh);
        String cachedSchedule=prefs.getString("schedule",todaySummary);if("个人课表".equals(cachedSchedule.trim())){cachedSchedule="今日无事";prefs.edit().putString("schedule",cachedSchedule).apply();}
        final float[] scheduleTouchStart={0f,0f};
        final int[] scheduleGestureAxis={0}; // 0=未判定，1=纵向，2=横向
        ScrollView scheduleScroll=new ScrollView(this){
            @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
                int action=event.getActionMasked();
                if(action==android.view.MotionEvent.ACTION_DOWN)scheduleTouchInProgress=true;
                try{return super.dispatchTouchEvent(event);}
                finally{if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_CANCEL)scheduleTouchInProgress=false;}
            }
            @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event){
                int action=event.getActionMasked();
                if(action==android.view.MotionEvent.ACTION_DOWN){
                    scheduleTouchStart[0]=event.getX();scheduleTouchStart[1]=event.getY();scheduleGestureAxis[0]=0;
                    getParent().requestDisallowInterceptTouchEvent(true);
                }else if(action==android.view.MotionEvent.ACTION_MOVE){
                    float dx=event.getX()-scheduleTouchStart[0],dy=event.getY()-scheduleTouchStart[1];
                    int slop=android.view.ViewConfiguration.get(MainActivity.this).getScaledTouchSlop();
                    if(scheduleGestureAxis[0]==0&&(Math.abs(dx)>slop||Math.abs(dy)>slop))scheduleGestureAxis[0]=Math.abs(dx)>Math.abs(dy)?2:1;
                    if(scheduleGestureAxis[0]==2){
                        // 横向手势不再切换日期；交给日期条自己的 HorizontalScrollView 处理滚动。
                        getParent().requestDisallowInterceptTouchEvent(true);
                        return false;
                    }
                    if(scheduleGestureAxis[0]==1){
                        boolean innerCanScroll=dy<0?canScrollVertically(1):canScrollVertically(-1);
                        getParent().requestDisallowInterceptTouchEvent(innerCanScroll);
                        if(!innerCanScroll)return false;
                    }
                }
                return super.onInterceptTouchEvent(event);
            }
            @Override public boolean onTouchEvent(android.view.MotionEvent event){
                int action=event.getActionMasked();
                if(action==android.view.MotionEvent.ACTION_UP){
                    getParent().requestDisallowInterceptTouchEvent(false);scheduleGestureAxis[0]=0;
                }else if(action==android.view.MotionEvent.ACTION_CANCEL){getParent().requestDisallowInterceptTouchEvent(false);scheduleGestureAxis[0]=0;}
                return super.onTouchEvent(event);
            }
        };
        scheduleScrollView=scheduleScroll;
        scheduleScroll.setFillViewport(false);scheduleScroll.setVerticalScrollBarEnabled(false);scheduleScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        LinearLayout scheduleContent=new LinearLayout(this);scheduleContent.setOrientation(LinearLayout.VERTICAL);scheduleScroll.addView(scheduleContent,new ScrollView.LayoutParams(-1,-2));
        schedule.addView(scheduleScroll,new LinearLayout.LayoutParams(-1,0,1));
        boolean showMultiDay=prefs.getBoolean("showMultiDaySchedule",true);
        if(showMultiDay){
            scheduleDayScroll=new HorizontalScrollView(this);scheduleDayScroll.setHorizontalScrollBarEnabled(false);scheduleDayScroll.setFillViewport(false);scheduleDayStrip=new LinearLayout(this);scheduleDayStrip.setOrientation(LinearLayout.HORIZONTAL);scheduleDayScroll.addView(scheduleDayStrip);LinearLayout.LayoutParams dayLp=new LinearLayout.LayoutParams(-1,dp(70));dayLp.topMargin=dp(12);dayLp.bottomMargin=dp(8);scheduleContent.addView(scheduleDayScroll,dayLp);
            String cachedDays=prefs.getString("scheduleDays","");if(!cachedDays.isEmpty()){String[] a=cachedDays.split(String.valueOf((char)30),-1);if(a.length==31)scheduleDays=a;else if(a.length==7){Calendar base=Calendar.getInstance();for(int i=0;i<31;i++){Calendar d=(Calendar)base.clone();d.add(Calendar.DAY_OF_MONTH,i-15);scheduleDays[i]=a[dayOfWeekIndex(d)];}}}
            scheduleValue=title("",17);scheduleValue.setVisibility(View.GONE);scheduleTimeline=new LinearLayout(this);scheduleTimeline.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams timelineLp=new LinearLayout.LayoutParams(-1,-2);timelineLp.topMargin=dp(5);scheduleContent.addView(scheduleTimeline,timelineLp);scheduleContent.addView(t("左右滑动日期条浏览日期，点击日期查看课表。",13,MUTED));renderScheduleDayStrip();updateScheduleDayDisplay();

        }else{
            scheduleDateLabel.setText("今天 · "+new SimpleDateFormat("M月d日 EEEE",Locale.CHINA).format(new Date()));
            scheduleValue=title(cachedSchedule,17);scheduleValue.setLineSpacing(dp(3),1.05f);scheduleContent.addView(scheduleValue);
        }
        return schedule;
    }
    private int dayOfWeekIndex(Calendar date){int d=date.get(Calendar.DAY_OF_WEEK);return d==Calendar.SUNDAY?6:d-2;}
    private String relativeDayLabel(int offset){if(offset==0)return "今天";if(offset==1)return "明天";if(offset==2)return "后天";if(offset==-1)return "昨天";if(offset==-2)return "前天";return offset>0?offset+"天后":(-offset)+"天前";}
    private Calendar scheduleDateAt(int index){Calendar d=Calendar.getInstance();d.set(Calendar.HOUR_OF_DAY,0);d.set(Calendar.MINUTE,0);d.set(Calendar.SECOND,0);d.set(Calendar.MILLISECOND,0);d.add(Calendar.DAY_OF_MONTH,index-15);return d;}
    private void renderScheduleDayStrip(){
        if(scheduleDayStrip==null)return;
        scheduleDayStrip.removeAllViews();
        for(int i=0;i<31;i++){
            Calendar d=scheduleDateAt(i);
            String label=new SimpleDateFormat("d",Locale.CHINA).format(d.getTime())+"\n"+new SimpleDateFormat("EE",Locale.CHINA).format(d.getTime());
            TextView b=new TextView(this);b.setText(label);b.setGravity(Gravity.CENTER);b.setTextSize(13);
            b.setTypeface(null,i==selectedScheduleIndex?Typeface.BOLD:Typeface.NORMAL);
            b.setTextColor(i==selectedScheduleIndex?Color.WHITE:MUTED);
            b.setBackground(i==selectedScheduleIndex?bg(Color.argb(220,63,139,235),16):bg(Color.argb(34,190,218,255),16));
            b.setPadding(dp(5),dp(7),dp(5),dp(7));b.setMinWidth(dp(48));b.setMinHeight(dp(62));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(48),dp(62));lp.rightMargin=dp(5);
            scheduleDayStrip.addView(b,lp);final int index=i;b.setOnClickListener(v->selectScheduleDay(index));
        }
        centerSelectedScheduleDay();
    }
    private void updateScheduleDayStripSelection(){
        if(scheduleDayStrip==null)return;
        for(int i=0;i<scheduleDayStrip.getChildCount();i++){
            View child=scheduleDayStrip.getChildAt(i);boolean selected=i==selectedScheduleIndex;
            if(child instanceof TextView){
                TextView day=(TextView)child;day.setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);
                day.setTextColor(selected?Color.WHITE:MUTED);
                day.setBackground(selected?bg(Color.argb(220,63,139,235),16):bg(Color.argb(34,190,218,255),16));
            }
        }
        centerSelectedScheduleDay();
    }
    private void centerSelectedScheduleDay(){
        if(scheduleDayScroll!=null&&scheduleDayStrip!=null)scheduleDayScroll.post(()->{
            View selected=scheduleDayStrip.getChildAt(selectedScheduleIndex);
            if(selected!=null)scheduleDayScroll.smoothScrollTo(Math.max(0,selected.getLeft()-(scheduleDayScroll.getWidth()-selected.getWidth())/2),0);
        });
    }
    private void updateScheduleDayDisplay(){
        if(scheduleDateLabel==null||scheduleValue==null)return;
        Calendar d=scheduleDateAt(selectedScheduleIndex);int offset=selectedScheduleIndex-15;
        scheduleDateLabel.setText(relativeDayLabel(offset)+"\n"+new SimpleDateFormat("M月d日 EEEE",Locale.CHINA).format(d.getTime()));
        scheduleDateLabel.setLineSpacing(dp(2),1.0f);
        String value=scheduleDays[selectedScheduleIndex];
        if(value==null||value.isEmpty())value=offset==0?prefs.getString("schedule","尚未同步课表"):"暂无课程";
        scheduleValue.setText(value);renderScheduleTimeline(value);
    }
    private void renderScheduleTimeline(String value){
        if(scheduleTimeline==null)return;
        scheduleTimeline.removeAllViews();
        if(value==null||value.trim().isEmpty()||value.contains("暂无课程")||value.contains("今日无事")||value.contains("尚未同步")){
            TextView empty=t("当天没有安排课程",15,MUTED);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(30),0,dp(30));scheduleTimeline.addView(empty);return;
        }
        String[] entries=value.replace("\\n","\n").split("\n");int count=0;
        for(String entry:entries){
            String line=entry.replaceFirst("^\\s*[•·]\\s*","").trim();
            if(line.isEmpty())continue;
            String period="";
            java.util.regex.Matcher pm=java.util.regex.Pattern.compile("^(1-2|3-4|5-6|7-8|9-10|11-12|13-14)\\s+").matcher(line);
            if(pm.find()){period=pm.group(1);line=line.substring(pm.end()).trim();}
            String course=line,teacher="",room="";
            line=line.replaceFirst("\\s+\\d+(?:-\\d+)?(?:\\s*,\\s*\\d+(?:-\\d+)?)*(?:\\s*[（(](?:单|双)[）)])?\\s+[\\d,]+$","").trim();
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(.*?)\\s+([^\\s@]+)\\s+@([^\\s]+)$").matcher(line);
            if(m.matches()){course=m.group(1).trim();teacher=m.group(2).trim();room="@"+m.group(3).trim();}
            else{java.util.regex.Matcher r=java.util.regex.Pattern.compile("^(.*?)\\s+@([^\\s]+)$").matcher(line);if(r.matches()){course=r.group(1).trim();room="@"+r.group(2).trim();}}
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.TOP);
            LinearLayout periodColumn=new LinearLayout(this);periodColumn.setOrientation(LinearLayout.VERTICAL);periodColumn.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);periodColumn.setPadding(0,dp(9),dp(5),0);
            TextView dot=t("●",12,BLUE);dot.setGravity(Gravity.CENTER);dot.setPadding(0,0,0,dp(3));periodColumn.addView(dot);
            TextView periodText=t(period.isEmpty()?"课程":"第"+period.replace("-", "–")+"节",11,MUTED);periodText.setGravity(Gravity.CENTER);periodText.setPadding(0,dp(2),0,0);periodColumn.addView(periodText);
            row.addView(periodColumn,new LinearLayout.LayoutParams(dp(66),-2));
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(15),dp(13),dp(15),dp(13));card.setBackground(glass(18));card.setElevation(dp(1));
            TextView title=t(course,16,Color.WHITE);title.setTypeface(null,Typeface.BOLD);title.setLineSpacing(dp(2),1.0f);card.addView(title);
            if(!teacher.isEmpty()){TextView tv=t(teacher,13,Color.rgb(174,192,216));tv.setPadding(0,dp(5),0,0);card.addView(tv);}
            if(!room.isEmpty()){TextView rv=t(room,13,Color.rgb(174,192,216));rv.setPadding(0,dp(2),0,0);card.addView(rv);}
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.bottomMargin=dp(9);row.addView(card,cp);scheduleTimeline.addView(row);count++;
        }
        if(count==0){TextView empty=t("当天没有安排课程",15,MUTED);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(30),0,dp(30));scheduleTimeline.addView(empty);}
    }
    private void selectScheduleDay(int index){if(index<0||index>30||index==selectedScheduleIndex)return;selectedScheduleIndex=index;updateScheduleDayStripSelection();updateScheduleDayDisplay();}
    private String[] splitCourseEntries(String rawDay){return CourseScheduleParser.splitCourseEntries(rawDay);}

    private boolean courseOccursInWeek(String line,int weekNo){return CourseScheduleParser.courseOccursInWeek(line,weekNo);}
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
            try{new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(value);prefs.edit().putString("semesterStart",value).apply();Toast.makeText(this,"开学日期已保存",Toast.LENGTH_SHORT).show();if(prefs.getBoolean("setupComplete",false))buildSettings();}catch(Exception e){Toast.makeText(this,"日期格式应为 YYYY-MM-DD",Toast.LENGTH_SHORT).show();}
        }).setNeutralButton("清除",(d,w)->{prefs.edit().remove("semesterStart").apply();Toast.makeText(this,"开学日期已清除",Toast.LENGTH_SHORT).show();if(prefs.getBoolean("setupComplete",false))buildSettings();}).show();
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
                int http=conn.getResponseCode();DiagnosticLogger.diagnostic(prefs,"每日一言","HTTP请求",http==200?"成功":"失败","HTTP状态码="+http);if(http!=200)throw new java.io.IOException("HTTP "+http);
                java.io.BufferedReader br=new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(),StandardCharsets.UTF_8));StringBuilder raw=new StringBuilder();String line;while((line=br.readLine())!=null)raw.append(line);br.close();
                String rawText=raw.toString();prefs.edit().putString("diag_hitokoto_raw",DiagnosticLogger.diagnosticText(rawText,1800)).apply();DiagnosticLogger.diagnostic(prefs,"每日一言","原始响应","已获取",DiagnosticLogger.diagnosticText(rawText,1800));org.json.JSONObject o=new org.json.JSONObject(rawText);String quote=o.optString("hitokoto","").trim();String from=o.optString("from","").trim();String who=o.optString("from_who","").trim();
                if("null".equalsIgnoreCase(from))from="";if("null".equalsIgnoreCase(who))who="";if(quote.isEmpty())return;
                String source="";if(!who.isEmpty()&&!from.isEmpty())source="—— "+who+" · "+from;else if(!who.isEmpty())source="—— "+who;else if(!from.isEmpty())source="—— "+from;
                prefs.edit().putString("hitokoto",quote).putString("hitokoto_from",source).putString("hitokoto_date",today).apply();DiagnosticLogger.diagnostic(prefs,"每日一言","JSON解析与缓存","成功","句子长度="+quote.length()+"；来源="+source);
                final String finalSource=source;runOnUiThread(()->{if(hitokotoValue!=null)hitokotoValue.setText(quote);if(hitokotoFrom!=null){hitokotoFrom.setText(finalSource);hitokotoFrom.setVisibility(finalSource.isEmpty()?View.GONE:View.VISIBLE);}});
            }catch(Exception e){DiagnosticLogger.diagnostic(prefs,"每日一言","请求或解析","失败",e.getClass().getSimpleName()+": "+e.getMessage());}finally{if(conn!=null)conn.disconnect();}
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

        LinearLayout semester=card();
        String semesterStart=prefs.getString("semesterStart","");
        int currentWeek=getSchoolWeek(Calendar.getInstance());
        String weekSummary=currentWeek>0?"当前按开学日期计算为第 "+currentWeek+" 周":"尚未设置开学日期，首页暂不显示周次";
        semester.addView(title("学期周次设置",18));
        semester.addView(t("设置本学期第一周周一的日期，应用会据此自动计算当前周次。",13,MUTED));
        semester.addView(t("开学日期："+(semesterStart.isEmpty()?"未设置":semesterStart)+"\n"+weekSummary,13,MUTED));
        Button semesterButton=new Button(this);
        semesterButton.setText(semesterStart.isEmpty()?"设置开学日期":"修改开学日期");
        semesterButton.setAllCaps(false);semesterButton.setTextColor(Color.WHITE);semesterButton.setBackground(glass(14));
        semesterButton.setOnClickListener(v->chooseSemesterStart());
        semester.addView(semesterButton,new LinearLayout.LayoutParams(-1,dp(48)));
        content.addView(semester);addSpace(content,12);

        LinearLayout reset=card();reset.setClickable(true);reset.setOnClickListener(v->confirmReset());reset.addView(title("初始化应用",18));reset.addView(t("清除本机登录状态、缓存的首页数据和首次设置状态，然后重新进入欢迎设置。",13,MUTED));content.addView(reset);addSpace(content,12);
        LinearLayout logs=card();logs.setClickable(true);logs.setOnClickListener(v->exportLogs());logs.addView(title("生成并保存日志",18));logs.addView(t("生成诊断日志并保存到手机 Downloads 目录。日志不会包含 Cookie、Token 或账号密码。",13,MUTED));content.addView(logs);addSpace(content,12);
        LinearLayout backup=card();backup.setClickable(true);backup.setOnClickListener(v->backupLoginState());backup.addView(title("备份",18));backup.addView(t("设置备份密码后，将登录状态加密导出到 Downloads。备份文件可用于换手机或卸载重装后恢复。",13,MUTED));content.addView(backup);addSpace(content,12);
        LinearLayout restore=card();restore.setClickable(true);restore.setOnClickListener(v->restoreLoginState());restore.addView(title("恢复",18));restore.addView(t("选择 .ecjtuee 备份文件并输入备份密码，恢复校园服务登录状态。",13,MUTED));content.addView(restore);

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(12),dp(6),dp(12),dp(8));nav.setBackgroundColor(Color.rgb(18,39,66));
        Button n1=navButton("⌂\n首页",false),n2=navButton("▦\n功能",false),n3=navButton("⚙\n设置",true);
        n1.setOnClickListener(v->showHome());n2.setOnClickListener(v->open("智慧教务",JWXT_OAUTH));n3.setOnClickListener(v->{});
        nav.addView(n1,new LinearLayout.LayoutParams(0,dp(72),1));nav.addView(n2,new LinearLayout.LayoutParams(0,dp(72),1));nav.addView(n3,new LinearLayout.LayoutParams(0,dp(72),1));

        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));root.addView(nav);applySystemBarInsets(root);setContentView(root);
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
            DiagnosticLogger.cleanupDiagnosticLogs(prefs);
            String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
            String fileName="ECJTUEE_Diagnostic_"+stamp+".txt";
            StringBuilder log=new StringBuilder();
            log.append("ECJTUEE 教务课表诊断日志\n");
            log.append("生成时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date())).append("\n");
            log.append("Android: ").append(android.os.Build.VERSION.RELEASE).append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n");
            log.append("设备: ").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append("\n");
            log.append("应用版本: 0.1.0\n");
            log.append("开学日期: ").append(prefs.getString("semesterStart","未设置")).append("\n");
            log.append("当前周次: ").append(getSchoolWeek(Calendar.getInstance())).append("\n");
            log.append("教务登录状态标记: ").append(prefs.getBoolean("jwxtLogged",false)).append("\n");
            log.append("最近抓取状态: ").append(lastJwxtCaptureStatus).append("\n");
            log.append("最近抓取时间: ").append(lastJwxtCaptureAt.isEmpty()?"无":lastJwxtCaptureAt).append("\n");
            log.append("最近一次缓存课表存在: ").append(prefs.contains("schedule")).append("\n");
            log.append("\n========== A. 教务原始抓取数据（按星期/节次分组） ==========\n");
            log.append((lastJwxtRawData.isEmpty()?prefs.getString("diag_jwxt_raw",""):lastJwxtRawData).isEmpty()?"暂无原始课表数据。请登录智慧教务并刷新首页后重新导出。":(lastJwxtRawData.isEmpty()?prefs.getString("diag_jwxt_raw",""):lastJwxtRawData)).append("\n");
            log.append("\n========== B. 应用解析后的课表 ==========\n");
            log.append((lastJwxtParsedData.isEmpty()?prefs.getString("diag_jwxt_parsed",""):lastJwxtParsedData).isEmpty()?"暂无解析结果。":(lastJwxtParsedData.isEmpty()?prefs.getString("diag_jwxt_parsed",""):lastJwxtParsedData)).append("\n");
            log.append("\n========== C. 当前缓存的31天课表 ==========\n");
            String packed=prefs.getString("scheduleDays","");
            if(packed.isEmpty())log.append("暂无缓存。\n");
            else{String[] cached=packed.split(String.valueOf((char)30),-1);for(int i=0;i<cached.length;i++){Calendar d=(Calendar)Calendar.getInstance().clone();d.add(Calendar.DAY_OF_MONTH,i-15);log.append(new SimpleDateFormat("yyyy-MM-dd EEE",Locale.CHINA).format(d.getTime())).append(": ").append(cached[i].replace("\n"," | ")).append("\n");}}
            log.append("\n========== D. 全应用操作事件（最近24小时） ==========\n");
            String events=DiagnosticLogger.pruneDiagnosticEvents(prefs.getString("diag_events",""),System.currentTimeMillis());
            log.append(events.isEmpty()?"暂无事件记录。\n":events);
            log.append("\n========== E. GPS 与 IP 定位诊断 ==========\n");
            log.append("IP 定位 API: https://ipwho.is/（仅记录接口主机，不记录完整请求 URL）\n");
            log.append("IP API HTTP 状态: ").append(prefs.getString("diag_ip_http","暂无请求记录")).append("\n");
            log.append("IP API 响应时间: ").append(prefs.getString("diag_ip_at","暂无")).append("\n");
            log.append("IP API 请求耗时: ").append(prefs.getString("diag_ip_duration_ms","暂无")).append("\n");
            log.append("IP API 响应正文（IP 地址字段已隐藏，最长保存 12000 字符）:\n");
            log.append(prefs.getString("diag_ip_raw","暂无 IP 定位接口响应。请在天气页面重试定位后立即导出日志。")).append("\n");
            log.append("GPS 定位、权限、Provider 状态、超时、IP 城市解析及气象站匹配过程，请查看 D 部分事件记录。事件保留最近 24 小时。\n");
            log.append("\n========== F. 中央气象台天气接口原始 JSON 响应 ==========\n");
            log.append("HTTP状态码: ").append(prefs.getString("diag_weather_http","暂无")).append("\n");
            log.append("响应时间: ").append(prefs.getString("diag_weather_at","暂无")).append("\n");
            log.append("原始响应正文（保留温度、体感温度及预报字段数值）:\n");
            log.append(prefs.getString("diag_weather_raw","暂无天气接口原始响应。请打开天气页面并成功刷新后重新导出。")).append("\n");
            log.append("\n========== G. 其他服务最近原始响应摘要 ==========\n");
            for(String key:new String[]{"diag_power_raw","diag_card_raw","diag_hitokoto_raw","diag_auth_status","diag_power_result","diag_card_result"}) log.append(key).append(": ").append(prefs.getString(key,"暂无")).append("\n");
            log.append("\n========== H. 诊断说明 ==========\n");
            log.append("A 部分记录网页课表表格中每个星期、每个节次单元格的文本；B 部分记录应用拆分后的课程条目；C 部分记录周次筛选后的本地缓存；D 部分记录最近 24 小时的应用事件；E 部分记录 GPS 与 IP 定位链路及 IP API 响应；F 部分记录中央气象台接口返回的天气 JSON 原文（最长保存 30000 个字符）。\n");
            log.append("未导出网页 HTML、Cookie、Token、账号密码、请求头或完整请求 URL。天气 JSON 会保留用于分析的温度数值，但可能包含站点和天气观测信息；原始课程文本可能包含课程名、教师姓名和教室信息，分享日志前请自行检查。\n");
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
                ContentValues values=new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME,fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE,"text/plain");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/ECJTUEE");
                values.put(MediaStore.MediaColumns.IS_PENDING,1);
                Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
                if(uri==null)throw new Exception("无法创建下载文件");
                try(OutputStream os=getContentResolver().openOutputStream(uri)){
                    if(os==null)throw new Exception("无法打开下载文件");
                    os.write(log.toString().getBytes(StandardCharsets.UTF_8));os.flush();
                }catch(Exception writeError){getContentResolver().delete(uri,null,null);throw writeError;}
                ContentValues ready=new ContentValues();ready.put(MediaStore.MediaColumns.IS_PENDING,0);
                getContentResolver().update(uri,ready,null,null);
            }else{
                File dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if(!dir.exists()&&!dir.mkdirs())throw new Exception("无法创建 Downloads 目录");
                File out=new File(dir,fileName);
                try(FileOutputStream os=new FileOutputStream(out)){os.write(log.toString().getBytes(StandardCharsets.UTF_8));}
            }
            Toast.makeText(this,"诊断日志已保存到 Downloads/ECJTUEE/"+fileName,Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"日志保存失败："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
    }

    private void refreshAllHomeInBackground(){
        if(backgroundRefreshing)return;
        ArrayList<String> visible=getWidgetOrder();boolean needSchedule=visible.contains("schedule"),needPower=visible.contains("power"),needCard=visible.contains("card")||visible.contains("spend"),needWeather=visible.contains("weather")&&prefs.getBoolean("weather_ready",false);
        int tasks=(needSchedule?1:0)+(needPower?1:0)+(needCard?1:0)+(needWeather?1:0);
        if(tasks==0){if(homeRefresh!=null)homeRefresh.setRefreshing(false);return;}
        backgroundRefreshing=true;backgroundRefreshPending=tasks;backgroundRefreshFailures=0;
        if(needSchedule)refreshHidden("https://jwxt.ecjtu.edu.cn/Schedule/Schedule_getUserSchedume.action?item=0205",0);
        if(needPower)refreshHidden("https://billsys.ecjtu.edu.cn/student/#/",1);
        if(needCard)refreshHidden("https://ecard.ecjtu.edu.cn/",2);
        if(needWeather)fetchCmaWeather(prefs.getString("weather_station",""),true);
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
                     String js=type==0?"(function(){var t=document.getElementById('courseSche');if(!t)return 'JWCAP|';var rows=t.querySelectorAll('tr'),days=[[],[],[],[],[],[],[]];function blocks(cell){var box=cell;for(var n=0;n<6&&box.children&&box.children.length===1;n++)box=box.children[0];var kids=box.children;if(kids&&kids.length>1){var out=[];for(var k=0;k<kids.length;k++){var tx=(kids[k].innerText||kids[k].textContent||'').replace(/\\r/g,'').trim();if(tx)out.push(tx);}if(out.length>1)return out;}var raw=(cell.innerText||cell.textContent||'').replace(/\\r/g,'').trim();return raw?[raw]:[];}for(var r=1;r<rows.length;r++){var c=rows[r].querySelectorAll('th,td');var slot=c.length?(c[0].innerText||'').trim():'';if(!/^(1-2|3-4|5-6|7-8|9-10|11-12|13-14)$/.test(slot))continue;for(var d=1;d<=7;d++){if(c.length<=d)continue;var parts=blocks(c[d]);for(var q=0;q<parts.length;q++){var val=parts[q];if(val&&val!=='-'&&val!=='无')days[d-1].push(slot+'\\t'+val);}}}return 'JWCAP7|'+days.map(function(a){return encodeURIComponent(a.join('\\n'));}).join('|');})()":type==1?"(function(){return 'BILLCAP|'+(document.body?document.body.innerText:'').slice(0,12000);})()":"(function(){return 'CARDCAP|'+(document.body?document.body.innerText:'').slice(0,12000);})()";
                    v.evaluateJavascript(js,x->{
                        if(finished[0])return;
                        boolean ok=isRefreshPayloadValid(x,type);
                        String module=type==0?"教务课表":type==1?"宿舍电费":"校园卡";String payload=jsClean(x);
                        DiagnosticLogger.diagnostic(prefs,module,"页面数据抓取",ok?"成功":"失败","返回长度="+payload.length()+"；摘要="+DiagnosticLogger.diagnosticText(payload,1800));
                        if(type==0&&!ok){String failed=jsClean(x);lastJwxtCaptureStatus="抓取失败或未识别到课表表格；页面可能未登录、未加载完成或表格结构变化";lastJwxtCaptureAt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date());lastJwxtRawData="原始返回内容（截取前3000字符）:\n"+failed.substring(0,Math.min(3000,failed.length()));}
                        if(ok)parseSync(x,type==0,type==1,type==2);else DiagnosticLogger.diagnostic(prefs,module,"数据校验","失败","未找到预期数据标记或关键字段");
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
            DiagnosticLogger.diagnostic(prefs,"首页","后台刷新汇总",backgroundRefreshFailures==0?"成功":"部分失败","失败项目数="+backgroundRefreshFailures);
            if(backgroundRefreshFailures>0)Toast.makeText(this,"首页刷新失败：有 "+backgroundRefreshFailures+" 项未能更新，请检查网络或登录状态",Toast.LENGTH_LONG).show();
        }
    }

    private Button navButton(String s,boolean active){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(active?Color.rgb(129,190,255):MUTED);b.setBackground(active?bg(Color.argb(54,92,165,255),16):bg(Color.TRANSPARENT,16));b.setMinHeight(dp(56));return b;}

    @SuppressLint("SetJavaScriptEnabled") private void ensureBrowser(){if(browserView!=null)return;browserView=new LinearLayout(this);browserView.setOrientation(LinearLayout.VERTICAL);browserView.setBackground(pageBackground());
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(4),dp(4),dp(4),dp(4));bar.setBackground(glass(20));bar.setElevation(dp(3));
        Button back=navButton("‹",true),home=navButton("首页",true),refresh=navButton("刷新",false);browserTitle=title("",15);browserTitle.setGravity(Gravity.CENTER);
        bar.addView(back,new LinearLayout.LayoutParams(dp(54),dp(54)));bar.addView(home,new LinearLayout.LayoutParams(dp(72),dp(54)));bar.addView(browserTitle,new LinearLayout.LayoutParams(0,dp(54),1));bar.addView(refresh,new LinearLayout.LayoutParams(dp(72),dp(54)));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);webView=new WebView(this);WebSettings s=webView.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);
        webView.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();String host=u.getHost();if(host==null)return true;boolean ok=allowed.contains(host)||host.endsWith(".ecjtu.edu.cn");return !ok;}@Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){DiagnosticLogger.diagnostic(prefs,"WebView","SSL错误","失败","主机="+(e.getUrl()==null?"未知":Uri.parse(e.getUrl()).getHost())+"；错误码="+e.getPrimaryError());h.cancel();}@Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())DiagnosticLogger.diagnostic(prefs,"WebView","页面加载","失败","主机="+r.getUrl().getHost()+"；错误码="+e.getErrorCode()+"；描述="+e.getDescription());}@Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame())DiagnosticLogger.diagnostic(prefs,"WebView","HTTP响应",e.getStatusCode()<400?"成功":"失败","主机="+r.getUrl().getHost()+"；状态码="+e.getStatusCode());}@Override public void onPageStarted(WebView v,String u,android.graphics.Bitmap icon){String host=u==null?"未知":Uri.parse(u).getHost();DiagnosticLogger.diagnostic(prefs,"WebView","页面开始加载","开始","主机="+host);}@Override public void onPageFinished(WebView v,String u){String host=u==null?"未知":Uri.parse(u).getHost();DiagnosticLogger.diagnostic(prefs,"WebView","页面加载完成","成功","主机="+host+"；标题="+v.getTitle());progress.setVisibility(View.GONE);CookieManager.getInstance().flush();syncFromPage(v,u);if(setupInProgress&&u!=null&&u.contains("jwxt.ecjtu.edu.cn/index.action")){browserTitle.setText("首次设置 · 正在配置");new android.os.Handler().postDelayed(()->webView.loadUrl(BILLSYS_OAUTH),500);return;}if(setupInProgress&&u!=null&&u.contains("billsys.ecjtu.edu.cn/student/")){setupInProgress=false;prefs.edit().putBoolean("setupComplete",true).putBoolean("billsysLogged",true).apply();Toast.makeText(MainActivity.this,"设置完成",Toast.LENGTH_SHORT).show();new android.os.Handler().postDelayed(()->showHome(),700);return;}boolean loggedPage=u!=null&&(u.contains("jwxt.ecjtu.edu.cn/index.action")||u.contains("ecard.ecjtu.edu.cn/"));if(loggedPage&&returnAfterLogin){returnAfterLogin=false;new android.os.Handler().postDelayed(()->showHome(),900);}}});
        webView.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView v,int p){progress.setVisibility(p<100?View.VISIBLE:View.GONE);progress.setProgress(p);}});
        back.setOnClickListener(v->{if(webView.canGoBack())webView.goBack();else showHome();});home.setOnClickListener(v->showHome());refresh.setOnClickListener(v->webView.reload());
        browserView.addView(bar);browserView.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));browserView.addView(webView,new LinearLayout.LayoutParams(-1,0,1));applySystemBarInsets(browserView);}
    private void open(String title,String url){open(title,url,true);}
    private void open(String title,String url,boolean returnHomeAfterLogin){ensureBrowser();browserTitle.setText(title);setContentView(browserView);browserView.setVisibility(View.VISIBLE);progress.setVisibility(View.VISIBLE);returnAfterLogin=returnHomeAfterLogin&&!setupInProgress&&url.contains("mobile.ecjtu.edu.cn/oauth/authorize")&&!url.equals(BILLSYS_OAUTH);webView.loadUrl(url);}
    private void showHome(){if(!prefs.getBoolean("setupComplete",false)){buildSetup();return;}buildHome();}
    private void syncFromPage(WebView v,String url){
        if(url==null)return;
        boolean jw=url.contains("jwxt.ecjtu.edu.cn"), bill=url.contains("billsys.ecjtu.edu.cn")||url.contains("atrustweb.ecjtu.edu.cn"), card=url.contains("ecard.ecjtu.edu.cn");
        if(!jw&&!bill&&!card)return;
        String js;
         if(jw) js="(function(){var t=document.getElementById('courseSche');if(!t)return 'JWCAP|';var rows=t.querySelectorAll('tr'),days=[[],[],[],[],[],[],[]];function blocks(cell){var box=cell;for(var n=0;n<6&&box.children&&box.children.length===1;n++)box=box.children[0];var kids=box.children;if(kids&&kids.length>1){var out=[];for(var k=0;k<kids.length;k++){var tx=(kids[k].innerText||kids[k].textContent||'').replace(/\\r/g,'').trim();if(tx)out.push(tx);}if(out.length>1)return out;}var raw=(cell.innerText||cell.textContent||'').replace(/\\r/g,'').trim();return raw?[raw]:[];}for(var r=1;r<rows.length;r++){var c=rows[r].querySelectorAll('th,td');var slot=c.length?(c[0].innerText||'').trim():'';if(!/^(1-2|3-4|5-6|7-8|9-10|11-12|13-14)$/.test(slot))continue;for(var d=1;d<=7;d++){if(c.length<=d)continue;var parts=blocks(c[d]);for(var q=0;q<parts.length;q++){var val=parts[q];if(val&&val!=='-'&&val!=='无')days[d-1].push(slot+'\\t'+val);}}}return 'JWCAP7|'+days.map(function(a){return encodeURIComponent(a.join('\\n'));}).join('|');})()";
        else if(bill) js="(function(){return 'BILLCAP|'+(document.body?document.body.innerText:'').slice(0,5000);})()";
        else js="(function(){return 'CARDCAP|'+(document.body?document.body.innerText:'').slice(0,5000);})()";
        v.evaluateJavascript(js,value->{String module=jw?"教务课表":bill?"宿舍电费":"校园卡";String payload=jsClean(value);DiagnosticLogger.diagnostic(prefs,module,"前台页面抓取","完成","返回长度="+payload.length()+"；摘要="+DiagnosticLogger.diagnosticText(payload,1500));parseSync(value,jw,bill,card);});
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
                    StringBuilder rawDiagnostic=new StringBuilder();
                    String[] weekdayNames={"星期一","星期二","星期三","星期四","星期五","星期六","星期日"};
                    for(int i=0;i<7;i++){
                        try{String decoded=java.net.URLDecoder.decode(encoded[i],"UTF-8").replace("\\r","").replace("\\\\n","\\n").replace("\\n","\\n");rawDiagnostic.append("【").append(weekdayNames[i]).append("】\n").append(decoded.isEmpty()?"（空）":decoded).append("\n");}catch(Exception ex){rawDiagnostic.append("【").append(weekdayNames[i]).append("】解码失败: ").append(ex.getClass().getSimpleName()).append("\n");}
                    }
                    lastJwxtRawData=rawDiagnostic.toString();prefs.edit().putString("diag_jwxt_raw",lastJwxtRawData).apply();
                    lastJwxtCaptureAt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date());
                    lastJwxtCaptureStatus="成功：读取到 JWCAP7，星期列数="+encoded.length;prefs.edit().putString("diag_jwxt_status",lastJwxtCaptureStatus).putString("diag_jwxt_at",lastJwxtCaptureAt).apply();
                    for(int i=0;i<7;i++){
                        try{weekdayCourses[i]=java.net.URLDecoder.decode(encoded[i],"UTF-8").trim();}catch(Exception ex){weekdayCourses[i]="";}
                        if(!weekdayCourses[i].isEmpty()){
                            String rawDay=weekdayCourses[i].replace("\\r","").replace("\\\\n","\n").replace("\\n","\n");

                            String[] entries=splitCourseEntries(rawDay);StringBuilder nice=new StringBuilder();
                            for(String line:entries){line=line.trim();if(line.isEmpty())continue;if(nice.length()>0)nice.append("\n");nice.append("• ").append(line);}
                            weekdayCourses[i]=nice.toString();
                        }
                    }
                    String[] days=new String[31];Calendar today=Calendar.getInstance();
                    for(int i=0;i<31;i++){
                        Calendar d=(Calendar)today.clone();d.add(Calendar.DAY_OF_MONTH,i-15);
                        String source=weekdayCourses[dayOfWeekIndex(d)];int weekNo=getSchoolWeek(d);StringBuilder visible=new StringBuilder();
                        if(!source.isEmpty()){for(String line:source.split("\\n")){line=line.trim();if(line.isEmpty()||!courseOccursInWeek(line,weekNo))continue;if(visible.length()>0)visible.append("\n");visible.append(line);}}
                        days[i]=visible.length()==0?(i==15?"今日无事":"暂无课程"):visible.toString();
                    }
                    StringBuilder parsedDiagnostic=new StringBuilder();
                    for(int i=0;i<7;i++){parsedDiagnostic.append("【").append(weekdayNames[i]).append("】\n");for(String item:splitCourseEntries(weekdayCourses[i]))parsedDiagnostic.append("  ").append(item).append("\n");}
                    lastJwxtParsedData=parsedDiagnostic.toString();prefs.edit().putString("diag_jwxt_parsed",lastJwxtParsedData).apply();
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
