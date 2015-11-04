package jp.co.iaccess.web.icms.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import jp.co.iaccess.web.icms.controller.auth.UserPrincipal;
import jp.co.iaccess.web.icms.model.entity.LogActionEntity;
import jp.co.iaccess.web.icms.model.entity.MstReportEntity;
import jp.co.iaccess.web.icms.service.log.ActionLogService;
import jp.co.iaccess.web.util.ScreenId;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 各画面で使用するユーティリティ
 * 共通サービスを使用する場合は、このクラスを介して実施します。
 * @author domyo
 *
 */
@Component
public abstract class ActionUtils {

	//logger
	protected Logger logger = Logger.getLogger(getClass());

	//システム使用文字コード とりあえずここに
	private static final String SYSTEM_CHARACTER_CD = "UTF-8";

	@Autowired
	protected UserPrincipal userPrincipal;
	@Autowired
	private ActionLogService actionLogService;

	@PostConstruct
	private void postConstract() {
		logger.info("********** start action " + getClass().getName() + " **********");
	}
	@PreDestroy
	private void preDestroy() {
		logger.info("********** end action " + getClass().getName() + " **********");
	}

	/**
	 * アクションログの登録
	 * @param userPrincipal
	 * @param cls
	 * @param operation
	 * @param contents
	 */
	protected void actionLog(UserPrincipal userPrincipal, String operation, String contents) {
		//画面IDの取得
		String screenId = getScreenId();
//		System.out.println("screenId = " + screenId);
		if (screenId == null) {
			//取得できない場合は実行しない
			return ;
		}
		//登録内容をセットして実行
		LogActionEntity param = new LogActionEntity();
		param.setIpAddress(userPrincipal.getIpAddress());
		param.setOperatorCd(userPrincipal.getOperatorCd());
		param.setOperatorName(userPrincipal.getOperatorName());
		param.setScreenId(screenId);
		param.setOperation(operation);
		param.setContents(contents);
		actionLogService.actionLog(param);
	}

	/**
	 * アクションログの登録
	 * @param userPrincipal
	 * @param cls
	 * @param operation
	 */
	protected void actionLog(UserPrincipal userPrincipal, String operation) {
		this.actionLog(userPrincipal, operation, null);
	}

	/**
	 * 帳票出力ログを作成
	 * @param userPrincipal
	 * @param report
	 * @param fileName
	 * @param contents
	 */
	protected void reportLog(UserPrincipal userPrincipal, MstReportEntity report, String fileName, String contents) {
		//帳票ID・帳票名がセットされていない場合は処理終了
		if (report == null) {
			return ;
		}
//		LogReportEntity param = new LogReportEntity();
//		param.setIpAddress(userPrincipal.getIpAddress());
//		param.setOperatorCd(userPrincipal.getOperatorCd());
//		param.setOperatorName(userPrincipal.getOperatorName());
//		param.setReportId(report.getReportId());
//		param.setReportName(report.getReportName());
//		param.setOutputFileName(fileName);
//		param.setContents(contents);
//		actionLogService.reportLog(param);
	}

	/**
	 * <p>ファイルダウンロード
	 * <p>ファイルのダウンロードを実施します。
	 * ヘッダーの書き換え、ファイルのダウンロードを行い、処理が終了したらファイルを削除してresponseを終了します。
	 * このメソッドの後に処理を実施することはできません。
	 * @param file
	 */
	protected void downloadFile(File file) {
		logger.info("file output start.");
		String fileName = "";
		String fileNameEncoded = "";
		String path = "";
		if (file != null) {
			fileName = file.getName();
			path = file.getPath();
			try {
				fileNameEncoded = URLEncoder.encode(fileName, SYSTEM_CHARACTER_CD);
			} catch (UnsupportedEncodingException e) {
				logger.error("URLEncoder error : " + e.getMessage());
				fileNameEncoded = fileName;
			}
			logger.info("output file name = " + fileNameEncoded);
		}
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext ec = context.getExternalContext();
		ec.responseReset();
		ec.setResponseContentType("application/octet-stream; charset=" + SYSTEM_CHARACTER_CD);
		ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileNameEncoded  + "\"");
		if (file != null) {
			ec.setResponseContentLength((int) file.length());
			try {
				output(file, ec.getResponseOutputStream());
			} catch (IOException e) {
				logger.error("output error : " + e.getMessage());
			}
			if (file.delete() == false) {
				logger.error("file delete failed. path = " + path + ", name = " + fileName);
			}
		} else {
			logger.warn("file is null.");
			ec.setResponseContentLength(0);
		}
		//処理完了
		logger.info("file output end.");
		context.responseComplete();
	}

	/**
	 * ファイルOutputStream処理
	 * @param file
	 * @param os
	 * @throws IOException
	 */
	private void output(File file, OutputStream os) throws IOException {
		byte buffer[] = new byte[4096];
		try (FileInputStream fis = new FileInputStream(file)) {
			int size;
			while ((size = fis.read(buffer)) != -1) {
				os.write(buffer, 0, size);
			}
		}
	}

	/**
	 * 画面IDを取得する
	 * @return
	 */
	protected String getScreenId() {
		Annotation[] as = getClass().getAnnotations();
		for (Annotation a: as) {
			if (a instanceof ScreenId) {
				ScreenId s = (ScreenId) a;
				return s.value();
			}
		}
		return null;
	}

}
