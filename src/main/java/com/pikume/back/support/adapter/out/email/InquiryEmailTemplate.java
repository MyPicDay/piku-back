package com.pikume.back.support.adapter.out.email;

import org.springframework.stereotype.Component;

@Component
public class InquiryEmailTemplate {

	private static final String SUBJECT = "[PikU] 피드백";
	private static final String CONTENT = """
			<html><body>
			<h2>피드백</h2>
			<p style='margin:10px 0;'>피드백 항목.</p>
			<p>피드백 내용: %s</p>
			<img src='cid:" + cid + "' style='width: 150px; margin-top: 20px;'/>
			<br>
			</body></html
			""";

	public String subject() {
		return SUBJECT;
	}

	public String render(String content) {
		return String.format(CONTENT, content);
	}
}
