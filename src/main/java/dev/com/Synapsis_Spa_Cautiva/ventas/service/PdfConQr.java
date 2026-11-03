package dev.com.Synapsis_Spa_Cautiva.ventas.service;

/** La representación impresa: un PDF con el QR y el hash del comprobante (RN07). */
public record PdfConQr(String nombreArchivo, byte[] contenido) {
}
