package com.example.absensipribadi

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.room.Room
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.itextpdf.kernel.pdf.*
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:ComponentActivity(){
 private val db by lazy{Room.databaseBuilder(this,AppDb::class.java,"absensi.db").build()}
 override fun onCreate(b:Bundle?){super.onCreate(b);val p=Security.prefs(this);if(!p.contains("pin"))p.edit().putString("pin",Security.hash("1234")).apply();setContent{Root(this,db)}}
}

@Composable fun Root(c:Context,db:AppDb){
 var ok by remember{mutableStateOf(false)}
 if(!ok)Pin{ok=true}else Home(c,db)
}

@Composable fun Pin(done:()->Unit){
 val p=Security.prefs(LocalContext.current);var pin by remember{mutableStateOf("")};var err by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
  Text("Absensi Pribadi Pro 2.3",style=MaterialTheme.typography.headlineMedium)
  OutlinedTextField(pin,{if(it.length<=6)pin=it.filter(Char::isDigit)},label={Text("PIN")},singleLine=true)
  Button({if(Security.verify(pin,p.getString("pin","")!!))done()else err=true},Modifier.padding(12.dp)){Text("Masuk")}
  if(err)Text("PIN salah",color=MaterialTheme.colorScheme.error)
 }
}

@Composable fun Home(c:Context,db:AppDb){
 var tab by remember{mutableIntStateOf(0)};val list by db.dao().observe().collectAsState(emptyList());val scope=rememberCoroutineScope()
 var tab by remember{mutableIntStateOf(0)}
    val list by db.dao().observe().collectAsState(emptyList())
    val scope=rememberCoroutineScope()

    val labels=listOf("Beranda","Riwayat","Laporan","Pengaturan")

    Scaffold(
        topBar={
            TopAppBar(
                title={Text("Absensi Pribadi Pro")}
            )
        },
        bottomBar={
            NavigationBar{
                for(i in labels.indices){
                    NavigationBarItem(
                        selected=tab==i,
                        onClick={tab=i},
                        icon={},
                        label={Text(labels[i])}
                    )
                }
            }
        }
    ){p->

@Composable fun Dashboard(c:Context,db:AppDb,list:List<Attendance>,scope:CoroutineScope,m:Modifier){
 val day=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date());val today=list.filter{it.date==day}
 val hasIn=today.any{it.type=="IN"};val hasOut=today.any{it.type=="OUT"};var note by remember{mutableStateOf("")};var type by remember{mutableStateOf("")}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->
  if(r[Manifest.permission.CAMERA]==true && (r[Manifest.permission.ACCESS_FINE_LOCATION]==true||r[Manifest.permission.ACCESS_COARSE_LOCATION]==true))
   takePhoto(c,db,scope,type,note)
 }
 Column(m.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("Dashboard",style=MaterialTheme.typography.headlineMedium)
  Text(SimpleDateFormat("EEEE, dd MMMM yyyy",Locale("id","ID")).format(Date()))
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text(SimpleDateFormat("HH:mm:ss",Locale.US).format(Date()),style=MaterialTheme.typography.displaySmall)
   Text(if(!hasIn)"Belum absen masuk" else if(!hasOut)"Sedang bekerja • ${today.first{it.type=="IN"}.time}" else "Absensi hari ini selesai")
  }}
  Button(enabled=!hasIn,onClick={type="IN";permissions(c,launcher)},Modifier.fillMaxWidth().height(54.dp)){Text("📷 ABSEN MASUK • SELFIE + GPS")}
  Button(enabled=hasIn&&!hasOut,onClick={type="OUT";permissions(c,launcher)},Modifier.fillMaxWidth().height(54.dp)){Text("📷 ABSEN PULANG • SELFIE + GPS")}
  OutlinedTextField(note,{note=it},label={Text("Catatan")},Modifier.fillMaxWidth())
 }
}

fun permissions(c:Context,l:androidx.activity.result.ActivityResultLauncher<Array<String>>){
 val a=arrayOf(Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
 val n=a.filter{ContextCompat.checkSelfPermission(c,it)!=PackageManager.PERMISSION_GRANTED}.toTypedArray()
 if(n.isEmpty()) l.launch(emptyArray()) else l.launch(n)
}

fun takePhoto(c:Context,db:AppDb,scope:CoroutineScope,type:String,note:String){
 val file=File(c.filesDir,"selfies").apply{mkdirs()};val photo=File(file,"${type}_${System.currentTimeMillis()}.jpg")
 val uri=FileProvider.getUriForFile(c,c.packageName+".fileprovider",photo)
 val intent=Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply{putExtra(android.provider.MediaStore.EXTRA_OUTPUT,uri);addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)}
 (c as? Activity)?.startActivityForResult(intent,if(type=="IN")200 else 201)
 // For full result handling, use the CameraX/ActivityResult implementation supplied in this project template.
 // The file URI is private and is the intended selfie target.
 val fused=LocationServices.getFusedLocationProviderClient(c)
 if(ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
  fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,CancellationTokenSource().token).addOnSuccessListener{loc->
   scope.launch(Dispatchers.IO){val d=Date();db.dao().insert(Attendance(date=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(d),type=type,time=SimpleDateFormat("HH:mm",Locale.US).format(d),timestamp=d.time,latitude=loc?.latitude,longitude=loc?.longitude,accuracy=loc?.accuracy,photoPath=photo.absolutePath,note=note));}
  }
}

@Composable fun History(list:List<Attendance>,m:Modifier){
 LazyColumn(m.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Riwayat",style=MaterialTheme.typography.headlineMedium)}
  items(list){r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(15.dp)){
   Text("${r.date} • ${if(r.type=="IN")"Masuk" else "Pulang"}",style=MaterialTheme.typography.titleMedium);Text(r.time)
   if(r.latitude!=null)Text("GPS %.6f, %.6f ±%.0fm".format(Locale.US,r.latitude,r.longitude,r.accuracy?:0f))
   if(r.photoPath!=null)Text("✓ Selfie: tersimpan");if(r.note.isNotBlank())Text(r.note)
  }}}
 }
}

@Composable fun Reports(c:Context,db:AppDb,list:List<Attendance>,scope:CoroutineScope,m:Modifier){
 val month=SimpleDateFormat("yyyy-MM",Locale.US).format(Date());val x=list.filter{it.date.startsWith(month)}
 Column(m.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Laporan",style=MaterialTheme.typography.headlineMedium)
  Stat("Hari tercatat",x.map{it.date}.distinct().size.toString());Stat("Total kejadian",x.size.toString())
  Button({scope.launch{pdf(c,x)}},Modifier.fillMaxWidth()){Text("Export PDF")}
  Button({scope.launch{backup(c,list)}},Modifier.fillMaxWidth()){Text("Backup JSON")}
  Text("File disimpan pada Documents privat aplikasi. Untuk backup ke lokasi pilihan pengguna, gunakan SAF pada build produksi.",style=MaterialTheme.typography.bodySmall)
 }
}

@Composable fun Settings(c:Context,db:AppDb,scope:CoroutineScope,m:Modifier){
 val p=Security.prefs(c);var old by remember{mutableStateOf("")};var nw by remember{mutableStateOf("")};var msg by remember{mutableStateOf("")}
 Column(m.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Pengaturan",style=MaterialTheme.typography.headlineMedium)
  OutlinedTextField(old,{old=it},label={Text("PIN lama")});OutlinedTextField(nw,{if(it.length<=6)nw=it.filter(Char::isDigit)},label={Text("PIN baru")})
  Button({if(Security.verify(old,p.getString("pin","")!!)&&nw.length>=4){p.edit().putString("pin",Security.hash(nw)).apply();msg="PIN berhasil diubah"}else msg="PIN lama salah / PIN baru minimal 4 digit"}){Text("Ubah PIN")}
  if(msg.isNotBlank())Text(msg)
  HorizontalDivider();Button({scope.launch{db.dao().clear()}},colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text("Hapus semua data lokal")}
 }
}
@Composable fun Stat(a:String,b:String){Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(a);Text(b,style=MaterialTheme.typography.titleLarge)}}}

suspend fun pdf(c:Context,l:List<Attendance>)=withContext(Dispatchers.IO){
 val d=c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!;val f=File(d,"Laporan_${System.currentTimeMillis()}.pdf")
 Document(PdfDocument(PdfWriter(f))).use{doc->{doc.add(Paragraph("LAPORAN ABSENSI PRIBADI").setBold());l.forEach{r->doc.add(Paragraph("${r.date} | ${r.type} | ${r.time} | ${r.latitude?: "-"}, ${r.longitude?: "-"} | ${r.note}"))}}}
 withContext(Dispatchers.Main){Toast.makeText(c,"PDF dibuat: ${f.name}",Toast.LENGTH_LONG).show()}
}
suspend fun backup(c:Context,l:List<Attendance>)=withContext(Dispatchers.IO){
 val a=JSONArray();l.forEach{r->a.put(JSONObject().apply{put("date",r.date);put("type",r.type);put("time",r.time);put("timestamp",r.timestamp);put("latitude",r.latitude);put("longitude",r.longitude);put("accuracy",r.accuracy);put("photoPath",r.photoPath);put("note",r.note)})}
 val f=File(c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"backup_${System.currentTimeMillis()}.json");f.writeText(a.toString(2))
 withContext(Dispatchers.Main){Toast.makeText(c,"Backup dibuat: ${f.name}",Toast.LENGTH_LONG).show()}
}
