package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// 1. اللون الأخضر الجديد: أخضر فاتح وهادئ وعصري (Light Sage / Modern Soft Emerald)
// يستخدم في الأزرار المهمة، زر المتابعة، العناصر المحددة، مؤشرات التشغيل، الأيقونات
val SoftEmerald = Color(0xFF10B981)
val SoftEmeraldDark = Color(0xFF059669)
val SoftEmeraldLight = Color(0xFF34D399)
val SoftEmeraldContainer = Color(0xFFE8F5E9)
val SoftEmeraldContainerDark = Color(0xFF064E3B)

// Aliases for compatibility
val EmeraldPrimary = SoftEmerald
val EmeraldPrimaryDark = SoftEmeraldDark
val EmeraldOnPrimary = Color.White
val EmeraldPrimaryContainer = SoftEmeraldContainer
val EmeraldOnPrimaryContainer = SoftEmeraldDark

// 2. اللون المميز الثانوي: ذهبي كهرماني هادئ
val GoldSecondary = Color(0xFFD97706)
val GoldOnSecondary = Color.White
val GoldSecondaryContainer = Color(0xFFFEF3C7)
val GoldOnSecondaryContainer = Color(0xFF92400E)

// 3. الوضع الفاتح (Light Mode): أبيض نقي مثل YouTube ورماديات خفيفة
val PureWhite = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFFFFFFF) // أبيض نقي تماماً كخلفية أساسية
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF2F2F2) // رمادي خفيف جداً للعناصر الثانوية والفواصل
val LightBorder = Color(0xFFE5E5E5)
val LightTextPrimary = Color(0xFF0F0F0F)
val LightTextSecondary = Color(0xFF606060)

// Backward compatibility aliases
val IvoryBackground = LightBackground
val CardWhite = PureWhite
val TextPrimaryCharcoal = LightTextPrimary
val TextSecondaryGreenGray = LightTextSecondary

// 4. الوضع الداكن (Dark Mode): رمادي داكن مريح للعين وليس أسود حاد (YouTube Dark Theme)
val DarkBackground = Color(0xFF0F0F0F) // خلفية داكنة مريحة للعين
val DarkSurface = Color(0xFF181818) // أسطح وعناصر
val DarkSurfaceVariant = Color(0xFF272727) // رمادي للأزرار الثانوية والقوائم
val DarkBorder = Color(0xFF383838) // فواصل خفيفة
val DarkTextPrimary = Color(0xFFF1F1F1) // نص أبيض مريح
val DarkTextSecondary = Color(0xFFAAAAAA) // رمادي فاتح للنصوص الثانوية
val DarkOnBackground = DarkTextPrimary
val DarkOnSurface = DarkTextPrimary
val DarkOnSurfaceVariant = DarkTextSecondary
