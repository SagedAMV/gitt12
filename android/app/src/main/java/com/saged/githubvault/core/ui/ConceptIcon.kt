package com.saged.githubvault.core.ui
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp

/** Native vectors translated from the approved HTML's embedded SVG paths. */
@Composable fun ConceptIcon(name:String,description:String?=null,modifier:Modifier=Modifier,tint:Color=LocalContentColor.current){
 val vector=remember(name){
  val paths=when(name){
   "stack"->listOf("M12 3 21 8 12 13 3 8 12 3Z","M3 12 12 17 21 12M3 16 12 21 21 16")
   "folder"->listOf("M3 7A2 2 0 0 1 5 5H10L12 8H19A2 2 0 0 1 21 10V19H3V7Z")
   "file"->listOf("M6 3H14L18 7V21H6V3Z","M14 3V8H18M9 13H15M9 17H13")
   "search"->listOf("M16 10A6 6 0 1 1 4 10A6 6 0 1 1 16 10Z","M15 15 20 20")
   "plus"->listOf("M12 5V19M5 12H19")
   "arrow"->listOf("M14 6 8 12 14 18")
   "back"->listOf("M10 6 16 12 10 18M16 12H4")
   "download"->listOf("M12 3V15M7 10 12 15 17 10M4 16V21H20V16")
   "upload"->listOf("M12 16V4M7 9 12 4 17 9M4 16V21H20V16")
   "work"->listOf("M6 5V19M3 16 6 19 9 16M18 19V5M15 8 18 5 21 8")
   "person"->listOf("M16 8A4 4 0 1 1 8 8A4 4 0 1 1 16 8Z","M4 21V19A8 8 0 0 1 20 19V21")
   "lock"->listOf("M8 10H16A3 3 0 0 1 19 13V18A3 3 0 0 1 16 21H8A3 3 0 0 1 5 18V13A3 3 0 0 1 8 10Z","M8 10V7A4 4 0 0 1 16 7V10M12 14V17")
   "alert"->listOf("M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12Z","M12 7V13M12 16V17")
   else->listOf("M5 12 9 16 19 6")
  }
  ImageVector.Builder(name,24.dp,24.dp,24f,24f).apply{paths.forEach{addPath(PathParser().parsePathString(it).toNodes(),fill=null,stroke=SolidColor(Color.Black),strokeLineWidth=1.7f,strokeLineCap=StrokeCap.Round,strokeLineJoin=StrokeJoin.Round)}}.build()
 }
 Icon(vector,description,modifier,tint)
}
