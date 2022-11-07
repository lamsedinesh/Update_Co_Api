package com.dinesh.service;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dinesh.entity.CitizenAppEntity;
import com.dinesh.entity.CoTriggerEntity;
import com.dinesh.entity.DcCaseEntity;
import com.dinesh.entity.EligDtlsEntity;

import com.dinesh.repository.CitizenAppRepository;
import com.dinesh.repository.CoTriggerRepository;
import com.dinesh.repository.DcCaseRepo;
import com.dinesh.repository.EligDtlsRepository;
import com.dinesh.response.CoResponse;
import com.dinesh.utils.EmailUtils;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class CoServiceImpl implements CoService {
	@Autowired
	private CoTriggerRepository coTrgRepo;

	@Autowired
	private EligDtlsRepository eligRepo;

	@Autowired
	private CitizenAppRepository appRepo;

	@Autowired
	private DcCaseRepo dcRepo;
	
	@Autowired
	private EmailUtils emailUtils;

	@Override
	public CoResponse processPendingTriggers() {
		CoResponse response = new CoResponse();
		
		CitizenAppEntity appEntity = null;
		// fetch all pending triggers
		List<CoTriggerEntity> pendingTrgs = coTrgRepo.findByTrgStatus("Pending");
		response.setTotolTriggers(Long.valueOf(pendingTrgs.size()));

		// Process each pending triggers
		for (CoTriggerEntity entity : pendingTrgs) {

			// get eligibility data based on casenum
			EligDtlsEntity elig = eligRepo.findByCaseNum(entity.getCaseNum());

			// get citizen data based on casenum
			Optional<DcCaseEntity> findById = dcRepo.findById(entity.getCaseNum());
			if (findById.isPresent()) {
				DcCaseEntity caseEntity = findById.get();
				Integer appId = caseEntity.getAppId();
				Optional<CitizenAppEntity> appEntityOptional = appRepo.findById(appId);
				if (appEntityOptional.isPresent()) {
					appEntity = appEntityOptional.get();
				}
			}
			// genrate pdf with elig details
			// send pdf to citizen mail
			
			Long failed = 0l;
			Long sucess = 0l;
			try {
				genrateAndSendPdf( elig , appEntity);
				sucess++;
			} catch (Exception e) {
				e.printStackTrace();
				failed++;
			}
			
			response.setSuccTriggers(sucess);
			response.setFailedTrigger(failed);
}
	return response;
	}

	
	private void genrateAndSendPdf(EligDtlsEntity eligData, CitizenAppEntity appEntity) throws Exception {

		Document document = new Document(PageSize.A4);
		File file = new File(eligData.getCaseNum() + ".pdf");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		PdfWriter.getInstance(document, fos);
		document.open();
		com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
		font.setSize(18);
		font.setColor(Color.BLUE);

		Paragraph p = new Paragraph("Eligibility Report", font);
		p.setAlignment(Paragraph.ALIGN_CENTER);

		document.add(p);

		PdfPTable table = new PdfPTable(7);
		table.setWidthPercentage(100f);
		table.setWidths(new float[] { 1.5f, 3.5f, 3.0f, 1.5f, 3.0f, 1.5f, 3.0f });
		table.setSpacingBefore(10);

		PdfPCell cell = new PdfPCell();
		cell.setBackgroundColor(Color.BLUE);
		cell.setPadding(5);

		font = FontFactory.getFont(FontFactory.HELVETICA);
		font.setColor(Color.WHITE);

		cell.setPhrase(new Phrase("Citizen Name", font));
		table.addCell(cell);

		cell.setPhrase(new Phrase("Plan Name", font));
		table.addCell(cell);

		cell.setPhrase(new Phrase("Plan Status", font));
		table.addCell(cell);

		cell.setPhrase(new Phrase("Plan Start Date", font));
		table.addCell(cell);

		cell.setPhrase(new Phrase("Plan End Date", font));
		table.addCell(cell);

		cell.setPhrase(new Phrase("Benifit Amt", font));
		table.addCell(cell);

		cell.setPhrase(new Phrase("Deniel Reason", font));
		table.addCell(cell);

	
			table.addCell(appEntity.getFullName());
			table.addCell(eligData.getPlanName());
			table.addCell(eligData.getPlanStatus());
			table.addCell(eligData.getPlanStartDate()+"");
			table.addCell(eligData.getPlanEndDate()+"");
			table.addCell(eligData.getBenifitAmt()+"");
			table.addCell(eligData.getDenialReason()+"");

		
		document.add(table);
		document.close();
		String subject = "HIS Eligibility Info";
		String body = "HIS Eligibility Info";
		
		emailUtils.sendEmail( appEntity.getEmail(), subject, body, file);
		updateTrigger(eligData.getCaseNum(), file);
		
		//file.delete();

	}
	
	private void updateTrigger(Long caseNum, File file) throws Exception {
		CoTriggerEntity coEntity = coTrgRepo.findByCaseNum(caseNum);
		long length = file.length();
		byte[] arr = new byte[(byte)file.length()];
		
		FileInputStream fis = new FileInputStream(file);
		fis.read(arr);
		
		coEntity.setCopdf(arr);
		
		coEntity.setTrgStatus("Completed");
		
		coTrgRepo.save(coEntity);
		
		fis.close();
	}

}
