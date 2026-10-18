package com.macoshapps.lootvault;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.net.*;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.util.*;
public class MainActivity extends Activity {
 final int BG=0xff0b111d,CARD=0xff192435,CARD2=0xff243854,BLUE=0xff6d9fff,WHITE=0xfff3f6fc,MUTED=0xff9baec4,GREEN=0xff46d69c,RED=0xffff797d,GOLD=0xffd3a54e;
 VaultStore store;LinearLayout root,body,bottom;String tab="home",filter="",lang="pl";JSONObject photoTarget;ImageView photoPreview;int topInset=0,bottomInset=0;String search="";
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 String tr(String pl,String en,String es){return lang.equals("en")?en:lang.equals("es")?es:pl;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(0x100|0x200|0x400);store=new VaultStore(this);lang=store.data.optString("language","pl");open("home");}
 GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 LinearLayout col(){LinearLayout x=new LinearLayout(this);x.setOrientation(1);return x;}
 TextView txt(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);if(bold)v.setTypeface(null,1);return v;}
 void add(LinearLayout l,View v){l.addView(v,new LinearLayout.LayoutParams(-1,-2));}
 void gap(LinearLayout l,int n){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
 void padding(View v,int n){v.setPadding(dp(n),dp(n),dp(n),dp(n));}
 TextView action(String s,Runnable r){TextView b=txt(s,15,BG,true);b.setGravity(Gravity.CENTER);b.setBackground(bg(GOLD,14));padding(b,16);b.setOnClickListener(v->r.run());return b;}
 void open(String page){
  tab=page;root=col();root.setBackgroundColor(BG);
  root.setOnApplyWindowInsetsListener((v,insets)->{int t=insets.getSystemWindowInsetTop(),b=insets.getSystemWindowInsetBottom();if(t!=topInset||b!=bottomInset){topInset=t;bottomInset=b;root.setPadding(0,t,0,b);}return insets;});
  setContentView(root);root.requestApplyInsets();
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
  body=col();body.setPadding(dp(18),dp(10),dp(18),dp(28));scroll.addView(body);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  if(page.equals("home"))home();else if(page.equals("assets"))assets();else if(page.equals("stats"))stats();else if(page.equals("settings"))settings();else if(page.equals("list"))list();else if(page.equals("detail"))detail();
  navigation();
 }
 void title(String small,String large){TextView eyebrow=txt(small.toUpperCase(java.util.Locale.ROOT),11,GOLD,true);add(body,eyebrow);gap(body,8);add(body,txt(large,29,WHITE,true));gap(body,18);}
 String money(double n){return String.format(java.util.Locale.forLanguageTag("pl-PL"),"%,.2f zł",n);}
 String signed(double n){return (n>0?"+":"")+money(n);}
 String pct(double cost,double value){if(cost<=0)return "—";return String.format(java.util.Locale.forLanguageTag("pl-PL"),"%+.1f%%",(value-cost)/cost*100);}
 double val(JSONObject x){return VaultStore.quantity(x)*VaultStore.price(x,"value");}
 double cost(JSONObject x){return VaultStore.quantity(x)*VaultStore.price(x,"purchase");}
 void card(String heading,String big,String sub,int subColor,Runnable click){
  LinearLayout c=col();padding(c,19);c.setBackground(bg(CARD,20));add(c,txt(heading,14,MUTED,false));gap(c,10);add(c,txt(big,27,WHITE,true));if(!sub.isEmpty()){gap(c,9);add(c,txt(sub,14,subColor,true));}
  add(body,c);gap(body,12);if(click!=null)c.setOnClickListener(v->click.run());
 }
 void tile(String heading,String detail,Runnable click){
  LinearLayout c=col();padding(c,17);c.setBackground(bg(CARD,16));add(c,txt(heading,18,WHITE,true));gap(c,7);add(c,txt(detail,13,MUTED,false));add(body,c);gap(body,11);c.setOnClickListener(v->click.run());
 }
 void section(String s){gap(body,12);add(body,txt(s,21,WHITE,true));gap(body,12);}
 void home(){
  title("LOOTVAULT",tr("Mój skarbiec","My vault","Mi tesoro"));
  double v=store.sum("value"),c=store.sum("purchase"),d=v-c;
  card(tr("Łączna wartość majątku","Total asset value","Valor total del patrimonio"),money(v),tr("Wynik względem zakupu: ","Return on cost: ","Resultado: ")+signed(d)+"  ("+pct(c,v)+")",d>=0?GREEN:RED,null);
  LinearLayout line=new LinearLayout(this);line.setOrientation(0);
  statBox(line,tr("Zasobów","Assets","Recursos"),""+store.realCount());statBox(line,tr("Typów","Types","Tipos"),""+store.types().length());
  add(body,line);gap(body,14);
  add(body,action(tr("+ Dodaj zasób","+ Add asset","+ Añadir recurso"),()->chooseType()));
  section(tr("Historia wartości","Value history","Historial de valor"));
  chart();
  section(tr("Ostatnio dodane","Recently added","Añadidos recientemente"));
  int shown=0;for(int i=store.items().length()-1;i>=0&&shown<4;i--){JSONObject x=store.items().optJSONObject(i);if(x.optBoolean("demo",false))continue;shown++;String id=x.optString("id");tile(x.optString("name"),money(val(x))+"  •  "+store.typeName(x.optString("type")),()->{detailId=id;open("detail");});}
  if(shown==0)add(body,txt(tr("Dodaj pierwszy zasób, aby rozpocząć monitorowanie majątku.","Add your first asset to start tracking wealth.","Añade tu primer recurso para seguir tu patrimonio."),14,MUTED,false));
 }
 void statBox(LinearLayout line,String label,String value){LinearLayout c=col();padding(c,16);c.setBackground(bg(CARD,15));add(c,txt(label,12,MUTED,false));gap(c,7);add(c,txt(value,22,WHITE,true));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(0,0,dp(8),0);line.addView(c,lp);}
 void chart(){JSONArray s=store.snapshots();if(s.length()<2){tile(tr("Brak wystarczającej historii","Not enough history","Historial insuficiente"),tr("Wykres pojawi się po kolejnych zmianach wartości.","Chart will appear after valuation updates.","El gráfico aparecerá tras nuevas valoraciones."),()->open("stats"));return;}WealthChart chart=new WealthChart(this,s);chart.setBackground(bg(CARD,18));body.addView(chart,new LinearLayout.LayoutParams(-1,dp(190)));gap(body,12);}

 void assets(){
  title("LOOTVAULT",tr("Twoje zasoby","Your assets","Tus recursos"));
  add(body,txt(tr("Wszystko, co posiadasz, w jednym miejscu","Everything you own, in one place","Todo lo que posees, en un solo lugar"),14,MUTED,false));gap(body,18);
  LinearLayout all=col();all.setPadding(dp(21),dp(22),dp(21),dp(22));all.setBackground(bg(0xff2a4169,23));
  add(all,txt("▤",27,0xffc4d8ff,true));gap(all,10);
  TextView allTitle=txt(tr("Wszystkie zasoby  ↗","All assets  ↗","Todos los recursos  ↗"),25,WHITE,true);add(all,allTitle);
  gap(all,8);add(all,txt(tr("Cała Twoja kolekcja","Your entire collection","Toda tu colección"),14,0xffcbd7e9,false));gap(all,15);
  add(all,txt(""+store.items().length(),30,WHITE,true));add(body,all);gap(body,15);
  all.setOnClickListener(v->{filter="";search="";open("list");});
  LinearLayout gridRow=null;JSONArray types=store.types();
  for(int i=0;i<types.length()+1;i++){
   if(i%2==0){gridRow=new LinearLayout(this);gridRow.setOrientation(0);add(body,gridRow);gap(body,12);}
   final boolean create=i==types.length();final JSONObject type=create?null:types.optJSONObject(i);
   final String id=create?"":type.optString("id");int count=0;
   if(!create)for(int j=0;j<store.items().length();j++)if(id.equals(store.items().optJSONObject(j).optString("type")))count++;
   LinearLayout card=col();card.setBackground(bg(CARD,20));card.setClipToOutline(true);
   if(create){
    LinearLayout empty=col();empty.setGravity(Gravity.CENTER);empty.setBackground(bg(0xff223149,18));empty.setMinimumHeight(dp(155));
    TextView plus=txt("⊕",38,0xffaac7ff,true);plus.setGravity(Gravity.CENTER);add(empty,plus);gap(empty,10);
    TextView label=txt(tr("Nowy typ","New type","Nuevo tipo"),18,WHITE,true);label.setGravity(Gravity.CENTER);add(empty,label);
    gap(empty,8);TextView sub=txt(tr("Utwórz własną kolekcję","Create a collection","Crea una colección"),12,MUTED,false);sub.setGravity(Gravity.CENTER);add(empty,sub);add(card,empty);
    card.setOnClickListener(v->newType());
   }else{
    ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setImageDrawable(new CoverDrawable(type.optString("name"),i));
    String encoded=type.optString("coverData");if(!encoded.isEmpty())try{byte[] b=android.util.Base64.decode(encoded,0);cover.setImageBitmap(BitmapFactory.decodeByteArray(b,0,b.length));}catch(Exception ignored){}
    card.addView(cover,new LinearLayout.LayoutParams(-1,dp(145)));
    LinearLayout details=col();details.setPadding(dp(13),dp(14),dp(12),dp(16));
    add(details,txt(type.optString("name"),17,WHITE,true));gap(details,7);
    add(details,txt(count+" "+tr("zasobów","assets","recursos"),13,MUTED,false));add(card,details);
    card.setOnClickListener(v->{filter=id;search="";open("list");});
    card.setOnLongClickListener(v->{editType(type);return true;});
   }
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(i%2==0)lp.setMargins(0,0,dp(6),0);else lp.setMargins(dp(6),0,0,0);
   gridRow.addView(card,lp);
  }
  gap(body,8);add(body,txt(tr("Przytrzymaj kafelek, aby zmienić jego nazwę lub zdjęcie.","Long-press a tile to change its name or cover.","Mantén pulsada una tarjeta para cambiar su nombre o portada."),12,MUTED,false));
 }
 class CoverDrawable extends android.graphics.drawable.Drawable {
  final String name;final int index;final Paint paint=new Paint(3);
  CoverDrawable(String n,int i){name=n.toLowerCase(java.util.Locale.ROOT);index=i;}
  @Override public void draw(Canvas c){
   android.graphics.Rect b=getBounds();float w=b.width(),h=b.height();int base=index%3==0?0xff25415d:index%3==1?0xff242b62:0xff59432e;
   paint.setShader(new android.graphics.LinearGradient(0,0,w,h,base,0xff0c1526,android.graphics.Shader.TileMode.CLAMP));c.drawRect(b,paint);paint.setShader(null);
   c.save();c.translate(b.left,b.top);
   if(name.contains("ksi")||name.contains("book")||name.contains("libro")){
    for(int i=0;i<7;i++){float x=w*(.07f+i*.135f);float top=h*(.22f+(i%3)*.07f);paint.setColor(new int[]{0xffba8553,0xff536f88,0xff7e5c67,0xffc1a678}[i%4]);c.drawRoundRect(x,top,x+w*.115f,h*.91f,dp(3),dp(3),paint);paint.setColor(0x88ffffff);c.drawRect(x+dp(4),top+dp(10),x+w*.115f-dp(4),top+dp(12),paint);}
   }else if(name.contains("gr")||name.contains("game")||name.contains("juego")||name.contains("aplik")){
    paint.setColor(0xff303f72);c.drawRoundRect(w*.16f,h*.22f,w*.85f,h*.78f,dp(23),dp(23),paint);paint.setColor(0xffa4bdf7);paint.setStrokeWidth(dp(5));float x=w*.37f,y=h*.49f;c.drawLine(x-dp(13),y,x+dp(13),y,paint);c.drawLine(x,y-dp(13),x,y+dp(13),paint);paint.setColor(0xffdca5d7);c.drawCircle(w*.69f,h*.43f,dp(6),paint);c.drawCircle(w*.75f,h*.55f,dp(6),paint);
   }else{
    paint.setColor(0xff3e5c6a);c.drawRoundRect(w*.1f,h*.28f,w*.42f,h*.82f,dp(12),dp(12),paint);paint.setColor(0xff718fa1);c.drawCircle(w*.26f,h*.43f,dp(17),paint);paint.setColor(0xff9e825a);c.drawRoundRect(w*.52f,h*.22f,w*.86f,h*.8f,dp(10),dp(10),paint);paint.setColor(0xffd2b78e);c.drawRect(w*.57f,h*.35f,w*.8f,h*.39f,paint);
   }
   c.restore();
  }
  @Override public void setAlpha(int a){}@Override public void setColorFilter(android.graphics.ColorFilter f){}@Override public int getOpacity(){return android.graphics.PixelFormat.OPAQUE;}
 }
 void editType(JSONObject type){
  LinearLayout form=col();padding(form,18);EditText name=edit(tr("Nazwa typu","Type name","Nombre del tipo"),type.optString("name"));add(form,name);gap(form,14);
  TextView coverButton=action(tr("Wybierz zdjęcie okładki","Choose cover image","Elegir portada"),()->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("image/*");in.addCategory(Intent.CATEGORY_OPENABLE);coverTarget=type;startActivityForResult(in,14);});add(form,coverButton);
  new AlertDialog.Builder(this).setTitle(tr("Edytuj typ zasobu","Edit asset type","Editar tipo de recurso")).setView(form).setPositiveButton(tr("Zapisz","Save","Guardar"),(d,w)->{try{if(!name.getText().toString().trim().isEmpty())type.put("name",name.getText().toString().trim());store.save();open("assets");}catch(Exception ignored){}}).setNegativeButton(tr("Anuluj","Cancel","Cancelar"),null).show();
 }
 JSONObject coverTarget;
 EditText edit(String hint,String val){EditText e=new EditText(this);e.setTextColor(WHITE);e.setHintTextColor(MUTED);e.setHint(hint);e.setTextSize(15);e.setSingleLine(true);e.setBackground(bg(CARD2,12));padding(e,14);e.setText(val);return e;}
 void field(LinearLayout form,String label,EditText e){add(form,txt(label,13,MUTED,false));gap(form,6);add(form,e);gap(form,13);}
 void list(){title("LOOTVAULT",filter.isEmpty()?tr("Wszystkie zasoby","All assets","Todos los recursos"):store.typeName(filter));
  EditText query=edit(tr("Szukaj po nazwie lub kategorii","Search name or category","Buscar nombre o categoría"),search);
  add(body,query);gap(body,14);query.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){search=s.toString();renderListItems();}public void afterTextChanged(android.text.Editable e){}});
  listHost=col();add(body,listHost);renderListItems();gap(body,10);add(body,action(tr("+ Dodaj zasób","+ Add asset","+ Añadir recurso"),()->{if(filter.isEmpty())chooseType();else form(filter,null);}));
 }
 LinearLayout listHost;void renderListItems(){if(listHost==null)return;listHost.removeAllViews();int found=0;for(int i=store.items().length()-1;i>=0;i--){JSONObject x=store.items().optJSONObject(i);if(!filter.isEmpty()&&!filter.equals(x.optString("type")))continue;String hay=(x.optString("name")+" "+x.optString("category")).toLowerCase(java.util.Locale.ROOT);if(!hay.contains(search.toLowerCase(java.util.Locale.ROOT)))continue;found++;String id=x.optString("id");double d=val(x)-cost(x);
   LinearLayout c=col();padding(c,15);c.setBackground(bg(CARD,15));add(c,txt(x.optString("name")+(x.optBoolean("demo",false)?"  • DEMO":""),17,WHITE,true));gap(c,5);add(c,txt(store.typeName(x.optString("type"))+"  •  "+money(val(x)),13,MUTED,false));gap(c,4);add(c,txt(signed(d)+"  "+pct(cost(x),val(x)),13,d>=0?GREEN:RED,true));add(listHost,c);gap(listHost,9);c.setOnClickListener(v->{detailId=id;open("detail");});
  }if(found==0)add(listHost,txt(tr("Brak pasujących zasobów","No matching assets","Sin recursos coincidentes"),14,MUTED,false));}
 String detailId="";
 void detail(){JSONObject x=store.byId(detailId);if(x==null){open("assets");return;}title(store.typeName(x.optString("type")),x.optString("name"));if(x.optString("photoData").length()>0){ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);try{byte[] bytes=android.util.Base64.decode(x.optString("photoData"),android.util.Base64.DEFAULT);image.setImageBitmap(BitmapFactory.decodeByteArray(bytes,0,bytes.length));body.addView(image,new LinearLayout.LayoutParams(-1,dp(220)));gap(body,15);}catch(Exception ignored){}}else if(x.optString("photo").length()>0){ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);try{image.setImageURI(Uri.parse(x.optString("photo")));body.addView(image,new LinearLayout.LayoutParams(-1,dp(220)));gap(body,15);}catch(Exception ignored){}}
  double c=cost(x),v=val(x),d=v-c;card(tr("Aktualna wartość","Current value","Valor actual"),money(v),signed(d)+"  ("+pct(c,v)+")",d>=0?GREEN:RED,null);
  tile(tr("Cena zakupu","Purchase cost","Coste de compra"),money(c),()->{});tile(tr("Ilość","Quantity","Cantidad"),""+VaultStore.quantity(x),()->{});
  tile(tr("Kategoria i lokalizacja","Category and location","Categoría y ubicación"),x.optString("category")+" • "+x.optString("location"),()->{});
  if(!x.optString("notes").isEmpty())tile(tr("Notatki","Notes","Notas"),x.optString("notes"),()->{});
  section(tr("Historia wycen","Valuation history","Historial de valoraciones"));JSONArray h=x.optJSONArray("history");
  if(h!=null){for(int i=h.length()-1;i>=0;i--){JSONObject entry=h.optJSONObject(i);String date=android.text.format.DateFormat.format("dd.MM.yyyy HH:mm",entry.optLong("time")).toString();tile(date,money(entry.optDouble("value"))+" / "+tr("szt.","unit","unidad"),()->{});}}
  add(body,action(tr("Dodaj / zmień zdjęcie","Add / change photo","Añadir / cambiar foto"),()->pickPhoto(x)));gap(body,10);
  add(body,action(tr("+ Nowa wycena","+ New valuation","+ Nueva valoración"),()->valuation(x)));gap(body,10);
  add(body,action(tr("Edytuj zasób","Edit asset","Editar recurso"),()->form(x.optString("type"),x)));gap(body,10);
  TextView del=txt(tr("Usuń zasób","Delete asset","Eliminar recurso"),14,RED,true);del.setGravity(Gravity.CENTER);padding(del,15);add(body,del);del.setOnClickListener(w->new AlertDialog.Builder(this).setMessage(tr("Usunąć ten zasób?","Delete this asset?","¿Eliminar este recurso?")).setPositiveButton(tr("Usuń","Delete","Eliminar"),(a,b)->{store.deleteItem(detailId);open("assets");}).setNegativeButton(tr("Anuluj","Cancel","Cancelar"),null).show());
 }
 void stats(){title("LOOTVAULT",tr("Statystyki","Statistics","Estadísticas"));double c=store.sum("purchase"),v=store.sum("value"),d=v-c;card(tr("Łączna wartość","Total value","Valor total"),money(v),signed(d)+"  ("+pct(c,v)+")",d>=0?GREEN:RED,null);
  tile(tr("Koszt nabycia","Acquisition cost","Coste de adquisición"),money(c),()->{});section(tr("Zmiany wartości majątku","Portfolio value history","Historial del patrimonio"));chart();section(tr("Podział majątku","Asset breakdown","Desglose del patrimonio"));
  for(int i=0;i<store.types().length();i++){JSONObject t=store.types().optJSONObject(i);String id=t.optString("id");double total=0,bought=0;for(int j=0;j<store.items().length();j++){JSONObject x=store.items().optJSONObject(j);if(id.equals(x.optString("type"))&&!x.optBoolean("demo",false)){total+=val(x);bought+=cost(x);}}double value=total,purchase=bought;tile(t.optString("name"),money(value)+"  •  "+signed(value-purchase)+"  ("+pct(purchase,value)+")",()->{filter=id;open("list");});}
 }
 void settings(){title("LOOTVAULT",tr("Ustawienia","Settings","Ajustes"));section(tr("Język","Language","Idioma"));tile("Polski / English / Español",lang.toUpperCase(java.util.Locale.ROOT),()->new AlertDialog.Builder(this).setItems(new String[]{"Polski","English","Español"},(d,i)->{lang=new String[]{"pl","en","es"}[i];try{store.data.put("language",lang);}catch(Exception ignored){}store.save();open("settings");}).show());
  section(tr("Kopia zapasowa","Backup","Copia de seguridad"));tile(tr("Eksportuj JSON","Export JSON","Exportar JSON"),tr("Zapisz dane i historię wycen","Save assets and valuation history","Guardar recursos e historial"),()->{Intent in=new Intent(Intent.ACTION_CREATE_DOCUMENT);in.setType("application/json");in.addCategory(Intent.CATEGORY_OPENABLE);in.putExtra(Intent.EXTRA_TITLE,"lootvault-backup.json");startActivityForResult(in,11);});
  tile(tr("Importuj JSON","Import JSON","Importar JSON"),tr("Przywróć dane z kopii","Restore saved data","Restaurar datos"),()->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("application/json");in.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(in,12);});
  section(tr("O aplikacji","About","Acerca de"));tile("LootVault","Macosh Apps • 0.4.0",()->{});
 }
 void navigation(){
  bottom=new LinearLayout(this);bottom.setBackgroundColor(CARD);bottom.setGravity(Gravity.CENTER_VERTICAL);
  String[] labels={tr("Pulpit","Home","Inicio"),tr("Zasoby","Assets","Recursos"),tr("Statystyki","Stats","Estadísticas"),tr("Ustawienia","Settings","Ajustes")};
  String[] icons={"⌂","▦","▥","⚙"};String[] tabs={"home","assets","stats","settings"};
  for(int i=0;i<4;i++){String dest=tabs[i];LinearLayout cell=col();cell.setGravity(Gravity.CENTER);TextView ico=txt(icons[i],25,tab.equals(dest)?GOLD:MUTED,true);ico.setGravity(Gravity.CENTER);add(cell,ico);gap(cell,3);TextView label=txt(labels[i],11,tab.equals(dest)?WHITE:MUTED,tab.equals(dest));label.setSingleLine(true);label.setGravity(Gravity.CENTER);add(cell,label);bottom.addView(cell,new LinearLayout.LayoutParams(0,dp(78),1));cell.setOnClickListener(v->open(dest));}
  root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(78)));
 }
 void newType(){EditText e=edit(tr("Nazwa typu zasobu","Asset type name","Nombre del tipo"),"");LinearLayout box=col();padding(box,20);add(box,e);new AlertDialog.Builder(this).setTitle(tr("Nowy typ","New type","Nuevo tipo")).setView(box).setPositiveButton(tr("Zapisz","Save","Guardar"),(d,i)->{if(!e.getText().toString().trim().isEmpty()){store.addType(e.getText().toString().trim());open("assets");}}).setNegativeButton(tr("Anuluj","Cancel","Cancelar"),null).show();}
 void chooseType(){JSONArray a=store.types();if(a.length()==0){new AlertDialog.Builder(this).setMessage(tr("Najpierw utwórz typ zasobu.","Create an asset type first.","Primero crea un tipo.")).setPositiveButton("OK",(d,w)->newType()).show();return;}String[] names=new String[a.length()];for(int i=0;i<a.length();i++)names[i]=a.optJSONObject(i).optString("name");new AlertDialog.Builder(this).setTitle(tr("Wybierz typ","Choose type","Elige tipo")).setItems(names,(d,i)->form(a.optJSONObject(i).optString("id"),null)).show();}
 double number(EditText e,double fallback){try{return Math.max(0,Double.parseDouble(e.getText().toString().trim().replace(",",".")));}catch(Exception ex){return fallback;}}
 void form(String type,JSONObject existing){
  ScrollView scroll=new ScrollView(this);LinearLayout box=col();padding(box,dp(18));scroll.addView(box);
  EditText name=edit(tr("Nazwa","Name","Nombre"),existing==null?"":existing.optString("name"));
  EditText cat=edit(tr("Kategoria","Category","Categoría"),existing==null?"":existing.optString("category"));
  EditText qty=edit(tr("Ilość","Quantity","Cantidad"),existing==null?"1":""+VaultStore.quantity(existing));
  EditText purchase=edit(tr("Cena zakupu za sztukę (zł)","Purchase price per unit (PLN)","Compra por unidad (PLN)"),existing==null?"":""+VaultStore.price(existing,"purchase"));
  EditText value=edit(tr("Wartość za sztukę (zł)","Current value per unit (PLN)","Valor por unidad (PLN)"),existing==null?"":""+VaultStore.price(existing,"value"));
  EditText loc=edit(tr("Miejsce przechowywania","Storage location","Ubicación"),existing==null?"":existing.optString("location"));
  EditText notes=edit(tr("Notatki","Notes","Notas"),existing==null?"":existing.optString("notes"));
  qty.setInputType(8194);purchase.setInputType(8194);value.setInputType(8194);
  field(box,tr("Nazwa","Name","Nombre"),name);field(box,tr("Kategoria","Category","Categoría"),cat);
  field(box,tr("Ilość","Quantity","Cantidad"),qty);field(box,tr("Cena zakupu / szt.","Purchase cost / unit","Coste / unidad"),purchase);
  field(box,tr("Aktualna wartość / szt.","Current value / unit","Valor / unidad"),value);
  field(box,tr("Miejsce przechowywania","Storage location","Ubicación"),loc);field(box,tr("Notatki","Notes","Notas"),notes);
  TextView pick=action(tr("Wybierz zdjęcie","Choose photo","Elegir foto"),()->{if(existing!=null)pickPhoto(existing);else Toast.makeText(this,tr("Najpierw zapisz zasób, potem dodaj zdjęcie.","Save the asset first, then add a photo.","Guarda el recurso antes de añadir foto."),1).show();});add(box,pick);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?tr("Nowy zasób","New asset","Nuevo recurso"):tr("Edytuj zasób","Edit asset","Editar recurso")).setView(scroll).setPositiveButton(tr("Zapisz","Save","Guardar"),null).setNegativeButton(tr("Anuluj","Cancel","Cancelar"),null).create();
  dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{if(name.getText().toString().trim().isEmpty()){name.setError(tr("Wpisz nazwę","Enter a name","Introduce un nombre"));return;}
   try{JSONObject x=existing==null?new JSONObject():existing;double old=VaultStore.price(x,"value"),current=number(value,0);if(existing==null){x.put("id",VaultStore.id());x.put("type",type);}
    x.put("demo",false);x.put("name",name.getText().toString().trim());x.put("category",cat.getText().toString());x.put("quantity",number(qty,1));x.put("purchase",number(purchase,0));x.put("value",current);x.put("location",loc.getText().toString());x.put("notes",notes.getText().toString());
    if(existing==null)store.items().put(x);if(existing==null||old!=current){JSONArray h=x.optJSONArray("history");if(h==null){h=new JSONArray();x.put("history",h);}JSONObject point=new JSONObject();point.put("time",System.currentTimeMillis());point.put("value",current);h.put(point);}
    store.snapshot();store.save();dialog.dismiss();filter=type;open("list");
   }catch(Exception e){Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();}
  }));dialog.show();
 }
 void pickPhoto(JSONObject x){photoTarget=x;Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("image/*");in.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(in,13);}
 void valuation(JSONObject x){EditText input=edit(tr("Nowa wartość za sztukę","New value per unit","Nuevo valor por unidad"),""+VaultStore.price(x,"value"));input.setInputType(8194);LinearLayout box=col();padding(box,20);add(box,input);
  new AlertDialog.Builder(this).setTitle(tr("Nowa wycena","New valuation","Nueva valoración")).setView(box).setPositiveButton(tr("Zapisz","Save","Guardar"),(d,i)->{try{double v=number(input,VaultStore.price(x,"value"));x.put("value",v);if(x.optBoolean("demo",false)){x.put("demo",false);x.put("category",x.optString("category").equals("Przykładowe")?"":x.optString("category"));}JSONArray h=x.optJSONArray("history");if(h==null){h=new JSONArray();x.put("history",h);}JSONObject p=new JSONObject();p.put("time",System.currentTimeMillis());p.put("value",v);h.put(p);store.snapshot();store.save();open("detail");}catch(Exception e){Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();}}).setNegativeButton(tr("Anuluj","Cancel","Cancelar"),null).show();
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();try{
  if(request==11){try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(store.exportJson().getBytes("UTF-8"));}Toast.makeText(this,tr("Kopia zapisana","Backup saved","Copia guardada"),0).show();}
  if(request==12){ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(uri)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}String json=out.toString("UTF-8");new AlertDialog.Builder(this).setMessage(tr("Import zastąpi obecne dane. Kontynuować?","Import replaces current data. Continue?","La importación reemplazará los datos. ¿Continuar?")).setPositiveButton("OK",(d,w)->{try{store.importJson(json);lang=store.data.optString("language","pl");open("home");}catch(Exception e){Toast.makeText(this,e.getMessage(),1).show();}}).setNegativeButton(tr("Anuluj","Cancel","Cancelar"),null).show();}
  if(request==14){if(coverTarget!=null){Bitmap source=BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));if(source==null)throw new Exception("Invalid cover");float scale=Math.min(1f,900f/Math.max(source.getWidth(),source.getHeight()));Bitmap scaled=Bitmap.createScaledBitmap(source,Math.max(1,(int)(source.getWidth()*scale)),Math.max(1,(int)(source.getHeight()*scale)),true);ByteArrayOutputStream out=new ByteArrayOutputStream();scaled.compress(Bitmap.CompressFormat.JPEG,82,out);coverTarget.put("coverData",android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP));store.save();open("assets");}}if(request==13){if(photoTarget!=null){Bitmap source=BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));if(source==null)throw new Exception("Invalid image");int w=source.getWidth(),h=source.getHeight();float scale=Math.min(1f,1024f/Math.max(w,h));Bitmap scaled=Bitmap.createScaledBitmap(source,Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)),true);ByteArrayOutputStream out=new ByteArrayOutputStream();scaled.compress(Bitmap.CompressFormat.JPEG,80,out);photoTarget.put("photoData",android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP));store.save();open("detail");}else Toast.makeText(this,tr("Najpierw zapisz zasób.","Save the asset first.","Primero guarda el recurso."),1).show();}
 }catch(Exception e){Toast.makeText(this,e.toString(),1).show();}}
}
