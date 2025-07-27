package store.piku.back.auth.constants;

public class EmailConstants {

    public static final String AUTH_CODE_CONTENT = """
            <html><body>
            <h2>이메일 인증</h2>
            <p style='margin:10px 0;'>안녕하세요. 나만의 캐릭터로 기록하는 하루 한 장, <b>PikU</b> 입니다.</p>
            <p>본인 인증을 위한 이메일 인증 코드는 다음과 같습니다.</p>
            <p style='font-size: 20px; font-weight: bold; color: #1a73e8; margin: 15px 0; letter-spacing: 2px;'> %s </p>
            <p>이 코드를 웹사이트에 입력하여 인증을 완료해주세요.</p>
            <br>
            </body></html>""";

    public static final String FEEDBACK = """
            <html><body>
            <h2>피드백</h2>
            <p style='margin:10px 0;'>피드백 항목.</p>
            <p>피드백 내용: %s</p>
            <img src='cid:" + cid + "' style='width: 150px; margin-top: 20px;'/>
            <br>
            </body></html
            """;

}
