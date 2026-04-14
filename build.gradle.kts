// Arquivo de build raiz onde você pode adicionar opções de configuração comuns a todos os submódulos. NÃO MUDAR
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}