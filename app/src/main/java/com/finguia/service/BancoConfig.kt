package com.finguia.service

/**
 * Mapa de pacotes de aplicativos bancários conhecidos para o nome amigável do banco.
 * Inclui os principais bancos, fintechs e carteiras digitais do Brasil.
 */
object BancoConfig {

    // Pacotes verificados contra as versões atuais das lojas/Play. Alguns bancos
    // têm múltiplos pacotes (varejo vs. empresa, versões legadas etc.) — todos
    // os pacotes reais que vimos em produção estão listados.
    val BANCOS_SUPORTADOS: Map<String, String> = mapOf(
        // Bancos tradicionais
        "br.com.bb.android"             to "Banco do Brasil",
        "com.itau"                      to "Itaú",
        "com.itau.empresas"             to "Itaú Empresas",
        "com.bradesco"                  to "Bradesco",
        "br.com.bradesco"               to "Bradesco",
        "com.bradesco.next"             to "Next",
        "com.santander.app"             to "Santander",
        "com.santander.way"             to "Santander Way",
        "br.com.gabba.Caixa"            to "Caixa Econômica",
        "br.gov.caixa.tem"              to "Caixa Tem",
        "br.com.sicoob"                 to "Sicoob",
        "br.com.sicredi.mbi"            to "Sicredi",
        "br.com.banrisul.banrisulmobile" to "Banrisul",
        "br.com.ailos.ia"               to "Ailos",
        "br.com.original.bank"          to "Banco Original",
        "com.bancoamazonia.mobilebanking" to "Banco da Amazônia",
        "com.btgpactual.banking"        to "BTG Pactual",

        // Fintechs e bancos digitais (pacotes atuais)
        "com.nu.production"             to "Nubank",
        "br.com.intermedium"            to "Banco Inter",
        "com.c6bank.app"                to "C6 Bank",
        "com.neon"                      to "Neon",
        "br.com.neon"                   to "Neon",
        "com.agibank.mobile"            to "Agibank",
        "com.willbank"                  to "Will Bank",
        "br.com.bs2.app"                to "BS2",
        "br.com.digio"                  to "Digio",
        "com.digio.app"                 to "Digio",
        "br.com.bv"                     to "BV",
        "com.stone.banking"             to "Stone",
        "com.stoneco.banking"           to "Stone",

        // Carteiras digitais e pagamentos
        "com.picpay"                    to "PicPay",
        "com.mercadopago.wallet"        to "Mercado Pago",
        "br.com.uol.ps.myaccount"       to "PagBank",
        "com.paypal.android.p2pmobile"  to "PayPal",
        "com.google.android.apps.walletnfcrel" to "Google Wallet",

        // Corretoras e investimentos
        "br.com.xpi.carteira"           to "XP",
        "br.com.rico.investimentos"     to "Rico",
        "br.com.clear"                  to "Clear",
        "com.nuinvest.app"              to "NuInvest",
        "br.com.avenue"                 to "Avenue",
        "br.com.toroinvestimentos"      to "Toro",

        // SMS (alguns bancos ainda enviam confirmação por SMS)
        "com.google.android.apps.messaging" to "Mensagens (SMS)",
        "com.samsung.android.messaging"     to "Mensagens (SMS)",
        "com.android.mms"                   to "SMS"
    )

    fun nomeBanco(pacote: String): String? = BANCOS_SUPORTADOS[pacote]

    fun ehBancoConhecido(pacote: String): Boolean = BANCOS_SUPORTADOS.containsKey(pacote)
}
