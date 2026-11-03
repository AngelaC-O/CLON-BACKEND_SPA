package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** La representación impresa del comprobante (RF102, RN07). Sin valor si el hash aún no llega: un PENDIENTE lo dice. */
@Component
public class GeneradorPdfComprobante {

	private static final int TAMANO_QR = 200;

	private static final String SEPARADOR_QR = "|";

	private static final String CODIGO_BOLETA = "03";

	private static final String CODIGO_FACTURA = "01";

	private final String ruc;

	private final String razonSocial;

	GeneradorPdfComprobante(@Value("${cautiva.empresa.ruc}") String ruc, @Value("${cautiva.empresa.razon-social}") String razonSocial) {
		this.ruc = ruc;
		this.razonSocial = razonSocial;
	}

	public PdfConQr generar(Comprobante oComprobante) {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		Document documento = new Document();
		try {
			PdfWriter.getInstance(documento, salida);
			documento.open();
			documento.add(new Paragraph(razonSocial));
			documento.add(new Paragraph("RUC: " + ruc));
			documento.add(new Paragraph(oComprobante.getTipo() + " ELECTRONICA " + oComprobante.getSerie() + "-" + oComprobante.getNumero()));
			documento.add(new Paragraph("Fecha de emision: " + oComprobante.getFechaEmision()));
			if (oComprobante.getNroDocComprador() != null) {
				documento.add(new Paragraph("Comprador: " + oComprobante.getNombreComprador() + " - " + oComprobante.getNroDocComprador()));
			}
			documento.add(new Paragraph("Importe: S/ " + oComprobante.getImporteVenta()));
			documento.add(new Paragraph("Estado SUNAT: " + oComprobante.getEstadoSunat()));
			documento.add(new Paragraph("Hash: " + (oComprobante.getHashCpe() == null ? "pendiente" : oComprobante.getHashCpe())));
			documento.add(Image.getInstance(png(generarQr(textoQr(oComprobante)))));
		}
		catch (DocumentException | IOException ex) {
			throw new IllegalStateException("No se pudo generar el PDF del comprobante", ex);
		}
		finally {
			documento.close();
		}
		return new PdfConQr(oComprobante.getSerie() + "-" + oComprobante.getNumero() + ".pdf", salida.toByteArray());
	}

	/** RUC|tipo|serie|número|IGV|importe|fecha|documento|hash: lo que lee el QR de una representación impresa. */
	public String textoQr(Comprobante oComprobante) {
		String codigo = oComprobante.getTipo() == TipoComprobante.BOLETA ? CODIGO_BOLETA : CODIGO_FACTURA;
		return String.join(SEPARADOR_QR, ruc, codigo, oComprobante.getSerie(), oComprobante.getNumero(), oComprobante.getImporteVenta().toPlainString(),
				String.valueOf(oComprobante.getFechaEmision()), oComprobante.getNroDocComprador() == null ? "" : oComprobante.getNroDocComprador(),
				oComprobante.getHashCpe() == null ? "" : oComprobante.getHashCpe());
	}

	public BufferedImage generarQr(String texto) {
		try {
			return MatrixToImageWriter.toBufferedImage(new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, TAMANO_QR, TAMANO_QR));
		}
		catch (WriterException ex) {
			throw new IllegalStateException("No se pudo generar el QR", ex);
		}
	}

	private static byte[] png(BufferedImage imagen) {
		try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			ImageIO.write(imagen, "png", salida);
			return salida.toByteArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
