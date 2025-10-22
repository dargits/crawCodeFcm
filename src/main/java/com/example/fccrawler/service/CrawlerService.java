package com.example.fccrawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import com.example.fccrawler.model.RedeemCode;

@Service
public class CrawlerService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6,20}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})(?:st|nd|rd|th)?\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REWARD_PATTERN = Pattern.compile("Reward[s]?:\\s*([^\\n]*?)(?=\\d{1,2}|$)", Pattern.CASE_INSENSITIVE);
    
    private static final Set<String> BLOCKLIST = new HashSet<>(Arrays.asList(
        "MAIN_MF", "REGION", "MHZSWN", "SITE-ROOT", "MASTERPAGE", "OAUB4", "PAGE-BG",
        "COMP-M1403WUT", "SECTION", "BGLAYERS", "MW5IWV", "COLORUNDERLAY", "VGO9YG",
        "IMAGEX", "LINKELEMENT", "H1DYHE", "QEBVG3", "UX33NC", "KVKPTZ", "NAVBAR",
        "MENU-ROOT", "QI0BCM", "YZGQBW", "MENU-ITEM", "DJGPM3", "MENU-ITEM-CONTENT",
        "MENU-ITEM-LINK", "YRGVFY", "DROPDOWN-ICON", "CF3HLB", "TNSPXD", "GROUP",
        "TOOLS", "DROPDOWN-CONTAINER", "SUBMENU", "DROPDOWN-ITEM", "UUKYLQ", "ZILXCF",
        "BV22OP", "SCROLL", "EUGVN8", "HANDLE-BUTTON", "LCZX5C", "COMP-ME3BZEE4",
        "RICHTEXTELEMENT", "_BLANK", "LIST", "LISTITEM", "BUTTONELEMENT",
        "COMP-M8A39S40", "COMP-M8A3BC7S", "COMP-M1ASZQYH", "COMP-M5F5TYN5",
        "COMP-M5F5ZGV6", "WIXGUARD", "WIX", "WIX_DATA", "WIDGET", "VIEWERSESSIONID",
        "WIDGETID", "WIDGETNAME", "WIXCODE", "VIEWER", "JSON", "SITE_CONTAINER",
        "TRUE", "FALSE", "LABEL", "PAGE", "SHAPE", "REWARD", "REWARDS", "PACK",
        "PACKS", "GEMS", "COIN", "COINS", "PLAYER", "PLAYERS", "STANDARD",
        "ANNIVERSARY", "LIMITED", "ITEM", "ITEMS", "CARD", "CARDS", "ACTIVE",
        "EXPIRED", "CODE", "CODES", "REDEEM", "ICON", "ICONS", "COPY", "DATE",
        "STATUS", "OVR", "RANK", "POINTS", "BUTTON", "HOME", "MORE", "CLOSE",
        // Common junk words
        "MOBILE", "LALIGA", "LEGENDS", "HALL", "SUCH", "HELPING", "GAMEPLAY",
        "DREAM", "TEAM", "BUILD", "MORE", "AND", "YOUR", "YOU", "ELEVATE",
        "WELCOME", "LATEST", "UPDATED", "MOST", "SPORTS", "OFFICIAL", "PARTNERS",
        "EXCLUSIVE", "REWARDS", "UNLOCK", "ELEVATE", "PACKS", "COINS", "GEMS",
        "IZTPZ8", "COMPARE", "CALCULATOR", "INVESTMENT", "TRAINING", "REVIEWS",
        "PROFILE", "HERE", "ABOUT", "CONTACT", "POLICY", "PRIVACY", "TERMS",
        // Expired/old codes - không phải JS hidden
        "NEWYEARNEWPACK", "HOLIDAYCHEER", "HOLIDAYGIFT", "THUNDERGIFT",
        "THEFANSTEAM", "THWINFCPRO", "OS11MELHORES", "REDENVELOPE", "ULTIMATEXI",
        "FRAGMENTOS", "TOTY25", "REDHEARTS", "SAMBA", "BRASILSILSIL", "RAMADANKAREEM",
        "OUSADIAEALEGRIA", "EVOLUTIONS", "PRESENTEDALIVE", "VAMOSLALIGA", "REGALOLALIGA",
        "LALIGAXFCMOBILE", "EIDMUBARAK25", "CODENEON", "DROPTHEMIC", "SIGANOSSOWHATS",
        "ROADTOMUNICH25", "FOLLOWWHITERABBIT", "PITCHBEATSLASTDANCE", "TOTSMOBILE25",
        "PREMIERLEAGUEGEMS", "RANKITUP", "FORZATOTS", "SHARDBOOST", "UTOTSREVEAL",
        "UTOTS25", "PICKASIDE", "VISAKIT", "NO1FCPRO", "CREATORLEGACY", "RAGNAROCKS",
        "WORLDWIDE", "RISEUP", "BOMDIA", "PLSUMMERUSA", "FCBAYERNVSPURS", "ULTIMATELEAGUEID",
        "TOUCHGRASS", "SKIPPER", "BRACADEIRA", "BUNDLEBOOST", "KROOSISYOURS", "FCPROSHANGHAI25",
        "SPEARVSHIELD", "BECKHAMISYOURS", "SONISYOURS", "ICONS26VIP", "SOCCERSATURDAY",
        "26ISLIVE", "CADEAUDOR", "FCM26NABGS", "CAMPEAOBRASILEIRO25", "AFICIONADO", "DAYONE",
        "JOGADORES", "JUGADORES", "NOSTALGIA", "ANYWHERE", "CREATORDYNASTY", "CLUBHOUSEVIP",
        "FCMBGS", "100KSEGUIDORESIG", "FC25CLUBHOUSE", "100KSEGUIDORESWA", "CEMPASUCHILYVELAS",
        "HALLOWEEN24", "DIADASBRUXAS", "TRICKORTREAT", "LIVELIBERTADORES", "FANZONE",
        "CLUBCLASH2024", "BALLONDORBR", "NOMINATE", "GRANDEFINAL", "SPS1", "FCPROFEST",
        // Partial/fragment codes
        "SXMHR2S", "SPBIMPW", "S__1KYOTA", "XKIJON", "HTN5RD", "ZPDMV1",
        "USMXR", "RTI1N", "AUTO", "YN8YY", "GP1LT"
    ));

    public List<RedeemCode> fetchCodes(String url) {
        WebDriver driver = null;
        try {
            System.out.println("🔄 Khởi động Selenium...");
            
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--disable-blink-features=AutomationControlled",
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            );
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.setExperimentalOption("useAutomationExtension", false);

            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

            System.out.println("Acess: " + url);
            driver.get(url);
            Thread.sleep(2000);


            ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('*').forEach(el => {" +
                "  el.style.display = 'block';" +
                "  el.style.visibility = 'visible';" +
                "  el.style.opacity = '1';" +
                "});"
            );
            
            Thread.sleep(1000);

            // Scroll
            System.out.println("Scrolling...");
            for (int i = 0; i < 5; i++) {
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, window.innerHeight);");
                Thread.sleep(500);
            }

            Thread.sleep(1000);

            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);
            
            List<RedeemCode> codes = extractCodesWithDetails(doc);
            
            System.out.println("find " + codes.size() + " codes");
            codes.forEach(c -> System.out.println("  " + c.getCode() + " | " + c.getReward() + " | " + c.getDate()));
            
            return codes;

        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Extract code với reward + date + status
     */
    private List<RedeemCode> extractCodesWithDetails(Document doc) {
        List<RedeemCode> results = new ArrayList<>();
        Map<String, RedeemCode> codeMap = new LinkedHashMap<>();

        System.out.println("Parsing code blocks...");

        Elements blocks = doc.select("*");
        
        for (Element block : blocks) {
            String blockText = block.text();
            
            // Filter: chỉ xem block có reward/pack
            if (!blockText.toLowerCase().contains("reward") && 
                !blockText.toLowerCase().contains("pack")) {
                continue;
            }

            // Skip expired
            if (blockText.toLowerCase().contains("expired")) {
                continue;
            }

            // Extract code từ block
            String code = extractCode(block);
            if (code == null || !isValidCode(code)) {
                continue;
            }

            // Skip nếu đã có
            if (codeMap.containsKey(code)) {
                continue;
            }

            // Extract reward
            String reward = extractReward(block, code);

            // Extract date
            String date = extractDate(block);

            String status = "Active";

            RedeemCode rc = new RedeemCode(code, reward, date, status, null);
            codeMap.put(code, rc);
            
            System.out.println("Code: " + code);
            System.out.println("Reward: " + reward);
            System.out.println("Date: " + date);
        }

        // Sort by date (mới nhất trước)
        results = codeMap.values().stream()
                .sorted((a, b) -> {
                    LocalDate dateA = parseDate(a.getDate());
                    LocalDate dateB = parseDate(b.getDate());
                    if (dateA != null && dateB != null) {
                        return dateB.compareTo(dateA); // Descending
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        return results;
    }

    /**
     * Extract code từ element
     */
    private String extractCode(Element block) {
        String html = block.html();
        
        // Cách 1: Quoted code
        Pattern quotedPattern = Pattern.compile("[\"']\\s*([A-Z0-9]{6,20})\\s*[\"']");
        Matcher quotedMatcher = quotedPattern.matcher(html);
        if (quotedMatcher.find()) {
            String code = quotedMatcher.group(1);
            if (isValidCode(code)) return code;
        }

        // Cách 2: ownText
        String ownText = block.ownText();
        if (ownText != null && !ownText.isBlank()) {
            Pattern codePattern = Pattern.compile("\\b([A-Z0-9]{6,20})\\b");
            Matcher codeMatcher = codePattern.matcher(ownText);
            if (codeMatcher.find()) {
                String code = codeMatcher.group(1);
                if (isValidCode(code)) return code;
            }
        }

        // Cách 3: Tìm trong children
        for (Element child : block.children()) {
            String code = extractCode(child);
            if (code != null) return code;
        }

        return null;
    }

    /**
     * Extract reward (text sau "Reward:")
     */
    private String extractReward(Element block, String code) {
        String text = block.text();
        
        // Tìm "Reward:" hoặc "Rewards:"
        int rewardIdx = text.toLowerCase().indexOf("reward");
        if (rewardIdx < 0) return null;

        // Tìm dấu ":" sau "Reward"
        int colonIdx = text.indexOf(":", rewardIdx);
        if (colonIdx < 0) colonIdx = rewardIdx + 6;
        
        int startIdx = colonIdx + 1;
        int endIdx = text.length();

        // Tìm date pattern (ví dụ: "22nd October", "20th October")
        String remaining = text.substring(startIdx);
        Matcher dateMatcher = DATE_PATTERN.matcher(remaining);
        if (dateMatcher.find()) {
            endIdx = startIdx + dateMatcher.start();
        }

        // Tìm code (ví dụ: "PARALLELPITCHES", "BRIGHTLIGHTS")
        int codeIdx = text.indexOf(code, startIdx);
        if (codeIdx > startIdx && codeIdx < endIdx) {
            endIdx = codeIdx;
        }

        // Extract substring
        if (startIdx >= text.length()) return null;
        
        String reward = text.substring(startIdx, Math.min(endIdx, text.length())).trim();
        
        // Clean up
        reward = reward.replaceAll("^[:\\s]+", "")  // Remove leading colons/spaces
                       .replaceAll("\\s+", " ")      // Normalize spaces
                       .trim();

        if (reward.isEmpty() || reward.length() > 150) return null;
        
        return reward;
    }

    /**
     * Extract date (format: "22nd October", "22 Oct", "20th October", etc)
     */
    private String extractDate(Element block) {
        String text = block.text();
        
        // Pattern: "22nd October", "20th October", "22 Oct", etc
        // Tìm date pattern sau "Reward: xxx"
        
        // Xóa phần trước "Reward:" để tránh extract sai
        int rewardIdx = text.toLowerCase().indexOf("reward");
        String afterReward = rewardIdx >= 0 ? text.substring(rewardIdx) : text;
        
        Matcher m = DATE_PATTERN.matcher(afterReward);
        if (m.find()) {
            return m.group(0);
        }

        return null;
    }

    /**
     * Parse date string to LocalDate (current year assumed)
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;

        try {
            // Remove ordinal suffix
            String clean = dateStr.replaceAll("(?:st|nd|rd|th)", "");

            // Parse with different formats
            String[] patterns = {
                "d MMMM",      // "22 October"
                "d MMM",       // "22 Oct"
                "dd MMMM",     // "22 October"
                "dd MMM"       // "22 Oct"
            };

            int currentYear = LocalDate.now().getYear();
            
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    LocalDate date = LocalDate.parse(clean, formatter)
                        .withYear(currentYear);
                    return date;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("Failed to parse date: " + dateStr);
        }

        return null;
    }

    /**
     * Check if token is valid code
     * - Must be 6-20 chars
     * - Only uppercase + digits
     * - At least 2 letters
     * - Not in blocklist
     * - Not single/double letter codes (like "A", "AB")
     */
    private boolean isValidCode(String code) {
        if (code == null || code.isBlank()) return false;
        if (code.length() < 6 || code.length() > 20) return false;
        if (BLOCKLIST.contains(code)) return false;
        if (!CODE_PATTERN.matcher(code).matches()) return false;
        if (code.matches("\\d{6,}")) return false;
        
        long letterCount = code.chars().filter(Character::isLetter).count();
        if (letterCount < 2) return false;
        
        // Reject if too many vowels in a row (likely not a code)
        if (code.matches(".*[AEIOU]{3,}.*")) return false;
        
        // Reject if starts with single letters that are common words
        if (code.matches("^[A-H]\\d{5,}.*")) return false;
        
        return true;
    }

    public List<String> fetchCodesWithJsoupOnly(String url) {
        return fetchCodes(url).stream()
                .map(RedeemCode::getCode)
                .collect(Collectors.toList());
    }

    public List<RedeemCode> fetchStructured(String url, boolean jsoupOnly) {
        return fetchCodes(url);
    }
}