package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import dev.com.Synapsis_Spa_Cautiva.comun.api.AlmacenArchivosPort;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioComprobante;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioFacturacionElectronica {

	private static final String SEPARADOR_QR = "|";

	private final RepositorioComprobante repositorioComprobante;

	private final FacturacionElectronicaPort facturacionElectronicaPort;

	private final AlmacenArchivosPort almacenArchivosPort;

	/** Si el proveedor no responde, el comprobante queda PENDIENTE y la venta se cierra igual (RN46). */
	@Transactional
	public Comprobante emitirComprobante(Long idComprobante) {
		Comprobante oComprobante = buscar(idComprobante);
		RespuestaCpe oRespuestaCpe = enviar(oComprobante);
		String base = "comprobantes/" + oComprobante.getSerie() + "-" + oComprobante.getNumero();
		String rutaXml = oRespuestaCpe.xml() == null ? null : almacenArchivosPort.guardar(base + ".xml", oRespuestaCpe.xml());
		String rutaCdr = oRespuestaCpe.cdr() == null ? null : almacenArchivosPort.guardar(base + "-cdr.xml", oRespuestaCpe.cdr());
		oComprobante.registrarRespuestaSunat(oRespuestaCpe.estadoSunat(), oRespuestaCpe.codigoRespuesta(), oRespuestaCpe.descripcionRespuesta(),
				oRespuestaCpe.hashCpe(), rutaXml, rutaCdr);
		return repositorioComprobante.saveAndFlush(oComprobante);
	}

	@Transactional(readOnly = true)
	public PdfConQr generarRepresentacionImpresa(Long idComprobante) {
		Comprobante oComprobante = buscar(idComprobante);
		String contenidoXml = oComprobante.getRutaXml() == null ? null : almacenArchivosPort.leer(oComprobante.getRutaXml());
		return new PdfConQr(idComprobante, oComprobante.getTipo(), oComprobante.getSerie(), oComprobante.getNumero(), oComprobante.getFechaEmision(),
				oComprobante.getNroDocComprador(), oComprobante.getNombreComprador(), oComprobante.getTotalVenta(), oComprobante.getEstadoSunat(),
				oComprobante.getHashCpe(), textoQr(oComprobante), contenidoXml);
	}

	/** Sin endpoint público todavía: es la ruta de la representación (D-12). */
	@Transactional(readOnly = true)
	public String generarEnlacePublico(Long idComprobante) {
		return "/api/v1/comprobantes/" + buscar(idComprobante).getIdComprobante() + "/representacion";
	}

	private RespuestaCpe enviar(Comprobante oComprobante) {
		try {
			return facturacionElectronicaPort.emitir(new DocumentoElectronico(oComprobante.getTipo().name(), oComprobante.getSerie(),
					oComprobante.getNumero(), oComprobante.getFechaEmision(), oComprobante.getIdTipoDocComprador(), oComprobante.getNroDocComprador(),
					oComprobante.getNombreComprador(), oComprobante.getTotalVenta()));
		}
		catch (RuntimeException ex) {
			log.warn("El comprobante {}-{} queda PENDIENTE: el proveedor no respondió", oComprobante.getSerie(), oComprobante.getNumero(), ex);
			return new RespuestaCpe(EstadoSunat.PENDIENTE, null, "El proveedor no respondió", null, null, null);
		}
	}

	/** Sin el RUC de la empresa, que no está en ninguna tabla (D-11). */
	private String textoQr(Comprobante oComprobante) {
		return String.join(SEPARADOR_QR, oComprobante.getTipo().name(), oComprobante.getSerie(), oComprobante.getNumero(),
				oComprobante.getTotalVenta().toPlainString(), String.valueOf(oComprobante.getFechaEmision()),
				String.valueOf(oComprobante.getNroDocComprador()), String.valueOf(oComprobante.getHashCpe()));
	}

	private Comprobante buscar(Long idComprobante) {
		return repositorioComprobante.findById(idComprobante)
			.orElseThrow(() -> new ReglaNegocioException("RF102", "El comprobante " + idComprobante + " no existe.", HttpStatus.NOT_FOUND));
	}

}
