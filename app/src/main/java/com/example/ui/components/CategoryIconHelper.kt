package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    fun getIcon(name: String): ImageVector {
        return when (name) {
            "Restaurant" -> Icons.Rounded.Restaurant
            "ShoppingCart" -> Icons.Rounded.ShoppingCart
            "DirectionsCar" -> Icons.Rounded.DirectionsCar
            "Store" -> Icons.Rounded.Store
            "Receipt" -> Icons.Rounded.Receipt
            "Movie" -> Icons.Rounded.Movie
            "FitnessCenter" -> Icons.Rounded.FitnessCenter
            "School" -> Icons.Rounded.School
            "Flight" -> Icons.Rounded.Flight
            "Face" -> Icons.Rounded.Face
            "AccountBalance" -> Icons.Rounded.AccountBalance
            "Home" -> Icons.Rounded.Home
            "Shield" -> Icons.Rounded.Shield
            "People" -> Icons.Rounded.People
            "Subscriptions" -> Icons.Rounded.Subscriptions
            "Work" -> Icons.Rounded.Work
            "LaptopMac" -> Icons.Rounded.LaptopMac
            "BusinessCenter" -> Icons.Rounded.BusinessCenter
            "TrendingUp" -> Icons.Rounded.TrendingUp
            "AttachMoney" -> Icons.Rounded.AttachMoney
            "Redeem" -> Icons.Rounded.Redeem
            "PhoneIphone" -> Icons.Rounded.PhoneAndroid
            "Flag" -> Icons.Rounded.Flag
            "Coffee" -> Icons.Rounded.Coffee
            "LocalGasStation" -> Icons.Rounded.LocalGasStation
            "MedicalServices" -> Icons.Rounded.MedicalServices
            "Pets" -> Icons.Rounded.Pets
            "SportsEsports" -> Icons.Rounded.SportsEsports
            "LocalHospital" -> Icons.Rounded.LocalHospital
            "Celebration" -> Icons.Rounded.Celebration
            else -> Icons.Rounded.Category
        }
    }

    fun parseColor(colorHex: String, fallback: Color = Color(0xFF64748B)): Color {
        return try {
            val clean = colorHex.removePrefix("#")
            val colorInt = clean.toLong(16)
            if (clean.length == 6) {
                Color(0xFF000000 or colorInt)
            } else if (clean.length == 8) {
                Color(colorInt)
            } else {
                fallback
            }
        } catch (e: Exception) {
            fallback
        }
    }
}
