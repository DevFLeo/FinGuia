package com.finguia.service

/**
 * Mapa de pacotes de aplicativos bancários conhecidos para o nome amigável do banco.
 * Inclui os principais bancos, fintechs e carteiras digitais do Brasil.
 */
object BancoConfig {

    val BANCOS_SUPORTADOS: Map<String, String> = mapOf(
        // Bancos tradicionais
        "br.com.bb.android"             to "Banco do Brasil",
        "com.itau"                      to "Itaú",
        "com.itau.empresas"             to "Itaú Empresas",
        "br.com.bradesco"               to "Bradesco",
        "com.santander.app"             to "Santander",
        "br.gov.caixa"                  to "Caixa Econômica",
        "com.hsbc.hsbcbrazil"           to "HSBC",
        "br.com.sicoobnet"              to "Sicoob",
        "br.com.sicredi"                to "Sicredi",
        "br.com.banrisul.mobile"        to "Banrisul",
        "br.com.ailos"                  to "Ailos",
        "com.modal.bank"                to "Banco Modal",
        "br.com.original.bank"          to "Banco Original",
        "br.com.bancoamazonia.app"      to "Banco da Amazônia",
        "br.com.bancoob.mobile"         to "Bancoob",

        // Fintechs e bancos digitais
        "com.nubank"                    to "Nubank",
        "br.com.intermedium"            to "Banco Inter",
        "com.c6bank.app"                to "C6 Bank",
        "br.com.next"                   to "Next",
        "com.neon.bank.personal"        to "Neon",
        "br.com.agibank.app"            to "Agibank",
        "br.com.sofisa.dindinapp"       to "Dindin (Sofisa)",
        "br.com.sofisa.bank"            to "Sofisa Direto",
        "com.Will.bank"                 to "Will Bank",
        "br.com.bs2.app"                to "BS2",
        "br.com.digio"                  to "Digio",
        "br.com.realizei.bank"          to "Realize Bank",
        "br.com.bv.apps.bvapp"          to "BV",
        "br.com.credsystem.app"         to "Cred System",
        "br.com.votorantim.bank"        to "Banco Votorantim",
        "com.stone.banking"             to "Stone",
        "br.com.boa.compra.app"         to "Boa Compra",

        // Carteiras digitais e pagamentos
        "com.picpay"                    to "PicPay",
        "com.mercadopago.wallet"        to "Mercado Pago",
        "br.com.uol.ps.myaccount"       to "PagBank",
        "com.paypal.android.p2pmobile"  to "PayPal",
        "br.com.getnet.tdv"             to "Getnet",
        "com.cielo.ondaemoda"           to "Cielo",
        "br.com.rede.app"               to "Rede",

        // Corretoras e investimentos
        "br.com.xp.carteira"            to "XP",
        "br.com.rico.comvc"             to "Rico",
        "br.com.clear.corretora"        to "Clear",
        "com.easynvest"                 to "Nuinvest",
        "br.com.avenue"                 to "Avenue",
        "br.com.toroinvestimentos.app"  to "Toro",

        // Apps de SMS que podem conter notificações bancárias
        "com.google.android.apps.messaging" to "Mensagens (SMS)",
        "com.android.mms"               to "SMS"
    )

    fun nomeBanco(pacote: String): String? = BANCOS_SUPORTADOS[pacote]

    fun ehBancoConhecido(pacote: String): Boolean = BANCOS_SUPORTADOS.containsKey(pacote)
}
