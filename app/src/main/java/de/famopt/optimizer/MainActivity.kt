package de.famopt.optimizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import java.nio.charset.Charset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme()) { AppNav() } }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home"){ HomeScreen { nav.navigate(it) } }
        composable("kita"){ KitaLuenenScreen() }
        composable("tax"){ TaxScreen() }
        composable("work"){ WorkLifeScreen() }
        composable("about"){ AboutScreen() }
    }
}

@Composable
fun HomeScreen(onNavigate: (String)->Unit){
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Familien Optimizer") }) }){ pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ){
            Text("Alles in einer App", fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            Text("Steuer • KiTa Lünen • Work-Life", lineHeight = 22.sp)
            Button(onClick = { onNavigate("kita") }, modifier = Modifier.fillMaxWidth()){ Text("KiTa Lünen (U3/Ü3)") }
            Button(onClick = { onNavigate("tax") },  modifier = Modifier.fillMaxWidth()){ Text("Steuer & Splitting (Schätzung)") }
            Button(onClick = { onNavigate("work") }, modifier = Modifier.fillMaxWidth()){ Text("Arbeitszeit- & Netto-Vergleich") }
            Button(onClick = { onNavigate("about") }, modifier = Modifier.fillMaxWidth()){ Text("Hinweise & Quellen") }
        }
    }
}

@Composable
fun KitaLuenenScreen(){
    val context = LocalContext.current
    val gson = remember { Gson() }
    val json = remember {
        context.assets.open("luenen_kita_2024.json").use { it.readBytes().toString(Charset.forName("UTF-8")) }
    }
    @Suppress("UNCHECKED_CAST")
    val tables = remember { gson.fromJson(json, Map::class.java) as Map<String, List<Map<String, Any>>> }

    var income by remember { mutableStateOf("50000") }
    var isUnder2 by remember { mutableStateOf(true) }
    var hours by remember { mutableStateOf(35) }

    fun lookup(): Int {
        val list = if (isUnder2) tables["under2"]!! else tables["over2"]!!
        val inc = income.toIntOrNull() ?: 0
        val match = list.firstOrNull { m ->
            val min = (m["min"] as Double).toInt()
            val max = (m["max"] as Double).toInt()
            inc in min..max
        }
        val key = when(hours){ 25 -> "25"; 45 -> "45"; else -> "35" }
        return (match?.get(key) as? Double)?.toInt() ?: 0
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("KiTa Lünen") }) }){ pv ->
        Column(
            Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ){
            OutlinedTextField(
                value = income, onValueChange = { income = it },
                label = { Text("Jahreseinkommen (EUR)") }, modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)){
                FilterChip(label={ Text("U3") }, selected=isUnder2, onClick={ isUnder2 = true })
                FilterChip(label={ Text("Ü3") }, selected=!isUnder2, onClick={ isUnder2 = false })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)){
                listOf(25,35,45).forEach { h ->
                    AssistChip(onClick = { hours = h }, label = { Text("$h Std./Woche") }, enabled = hours!=h)
                }
            }
            Divider()
            Text("Monatlicher Elternbeitrag: ${lookup()} €", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Text("Quelle: Satzung Lünen (ab 01.08.2024). Vorschuljahr beitragsfrei; Geschwisterbefreiung möglich.")
        }
    }
}

@Composable
fun TaxScreen(){
    var a by remember { mutableStateOf("45000") }
    var b by remember { mutableStateOf("24000") }
    val total = (a.toDoubleOrNull() ?: 0.0) + (b.toDoubleOrNull() ?: 0.0)
    val single = TaxEstimator2025.incomeTaxSingle(total)
    val split = TaxEstimator2025.incomeTaxMarried(total)

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Steuer (Schätzung)") }) }){ pv ->
        Column(Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)){
            OutlinedTextField(value=a, onValueChange={ a = it }, label={ Text("Einkommen A (zvE)") }, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=b, onValueChange={ b = it }, label={ Text("Einkommen B (zvE)") }, modifier=Modifier.fillMaxWidth())
            Divider()
            Text("Ohne Splitting: %.2f €".format(single))
            Text("Mit Splitting: %.2f €".format(split), fontWeight = FontWeight.SemiBold)
            Text("Vorteil: %.2f €".format(single - split), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text("Hinweis: Näherungsformel; Soli/KiSt nicht berücksichtigt.")
        }
    }
}

@Composable
fun WorkLifeScreen(){
    var hA by remember { mutableStateOf(40) }
    var hB by remember { mutableStateOf(20) }
    var pA by remember { mutableStateOf("25") }
    var pB by remember { mutableStateOf("18") }
    val grossA = hA * 4.33 * (pA.toDoubleOrNull()?:0.0)
    val grossB = hB * 4.33 * (pB.toDoubleOrNull()?:0.0)
    val zvE = (grossA + grossB) * 12 * 0.75
    val tax = TaxEstimator2025.incomeTaxMarried(zvE)
    val net = (zvE - tax) / 12.0

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Work-Life & Netto") }) }){ pv ->
        Column(Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)){
            Text("Partner A"); Slider(value=hA.toFloat(), onValueChange={ hA = it.toInt() }, valueRange=0f..40f, steps=39)
            OutlinedTextField(value=pA, onValueChange={ pA = it }, label={ Text("Stundenlohn A (€)") }, modifier=Modifier.fillMaxWidth())
            Text("Partner B"); Slider(value=hB.toFloat(), onValueChange={ hB = it.toInt() }, valueRange=0f..40f, steps=39)
            OutlinedTextField(value=pB, onValueChange={ pB = it }, label={ Text("Stundenlohn B (€)") }, modifier=Modifier.fillMaxWidth())
            Divider(); Text("Monatsnetto (Schätzung): %.0f €".format(net), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun AboutScreen(){
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Hinweise & Quellen") }) }){ pv ->
        Column(Modifier.padding(pv).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)){
            Text("KiTa Lünen: Satzung/Anlagen (ab 01.08.2024)")
            Text("Wohngeld: BMWSB/NRW – extern prüfen")
            Text("Steuer: §32a EStG (vereinfacht)")
        }
    }
}
