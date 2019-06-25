package org.tron.core.services.http;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.IvkDecryptAndMarkParameters;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

@Component
@Slf4j(topic = "API")
public class ScanAndMarkNoteByIvkServlet extends HttpServlet {
	
	@Autowired
	private Wallet wallet;
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		try {
			String input = request.getReader().lines()
					.collect(Collectors.joining(System.lineSeparator()));
			Util.checkBodySize(input);
			boolean visible = Util.getVisiblePost(input);
			IvkDecryptAndMarkParameters.Builder ivkDecryptParameters =
					IvkDecryptAndMarkParameters.newBuilder();
			JsonFormat.merge(input, ivkDecryptParameters);
			
			GrpcAPI.DecryptNotesMarked notes = wallet
					.scanAndMarkNoteByIvk(ivkDecryptParameters.getStartBlockIndex(),
							ivkDecryptParameters.getEndBlockIndex(),
							ivkDecryptParameters.getIvk().toByteArray(),
							ivkDecryptParameters.getAk().toByteArray(),
							ivkDecryptParameters.getNk().toByteArray());
			
			response.getWriter()
					.println(JsonFormat.printToString(notes, visible));
		} catch (Exception e) {
			logger.debug("Exception: {}", e.getMessage());
			try {
				response.getWriter().println(Util.printErrorMsg(e));
			} catch (IOException ioe) {
				logger.debug("IOException: {}", ioe.getMessage());
			}
		}
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		try {
			long startNum = Long.parseLong(request.getParameter("start_block_index"));
			long endNum = Long.parseLong(request.getParameter("end_block_index"));
			String ivk = request.getParameter("ivk");
			String ak = request.getParameter("ak");
			String nk = request.getParameter("nk");
			boolean visible = Util.getVisible(request);
			
			GrpcAPI.DecryptNotesMarked notes = wallet
					.scanAndMarkNoteByIvk(startNum, endNum, ByteArray.fromHexString(ivk),
							ByteArray.fromHexString(ak),ByteArray.fromHexString(nk));
			response.getWriter()
					.println(JsonFormat.printToString(notes, visible));
		} catch (Exception e) {
			logger.debug("Exception: {}", e.getMessage());
			try {
				response.getWriter().println(Util.printErrorMsg(e));
			} catch (IOException ioe) {
				logger.debug("IOException: {}", ioe.getMessage());
			}
		}
	}
}
