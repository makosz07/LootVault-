package com.macoshapps.lootvault;
import android.content.*;import android.graphics.*;import android.view.*;import org.json.*;
public final class WealthChart extends View{
 private final JSONArray series;private final Paint p=new Paint(3);private final int line=0xff71a8ff;
 public WealthChart(Context c,JSONArray data){super(c);series=data;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
 private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);int n=series.length();float l=dp(16),r=getWidth()-dp(16),t=dp(18),b=getHeight()-dp(24);p.setColor(0xff36445a);p.setStrokeWidth(dp(1));for(int i=0;i<4;i++){float y=t+(b-t)*i/3f;canvas.drawLine(l,y,r,y,p);}if(n==0)return;double lo=Double.MAX_VALUE,hi=-Double.MAX_VALUE;for(int i=0;i<n;i++){double v=series.optJSONObject(i).optDouble("value");lo=Math.min(lo,v);hi=Math.max(hi,v);}if(lo==hi){lo-=1;hi+=1;}double padding=(hi-lo)*.12;lo-=padding;hi+=padding;p.setColor(line);p.setStrokeWidth(dp(3));p.setStyle(Paint.Style.STROKE);Path path=new Path();for(int i=0;i<n;i++){float x=n==1?(l+r)/2:l+(r-l)*i/(n-1f);float y=(float)(b-(series.optJSONObject(i).optDouble("value")-lo)/(hi-lo)*(b-t));if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}canvas.drawPath(path,p);p.setStyle(Paint.Style.FILL);p.setTextSize(dp(11));p.setColor(0xffcbd6e6);canvas.drawText(String.format(java.util.Locale.US,"%.0f zł",hi-padding),l,t-dp(5),p);}
}
