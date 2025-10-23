package com.example.fccrawler.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
        "MOBILE", "LALIGA", "LEGENDS", "HALL", "SUCH", "HELPING", "GAMEPLAY",
        "DREAM", "TEAM", "BUILD", "MORE", "AND", "YOUR", "YOU", "ELEVATE",
        "WELCOME", "LATEST", "UPDATED", "MOST", "SPORTS", "OFFICIAL", "PARTNERS",
        "EXCLUSIVE", "REWARDS", "UNLOCK", "ELEVATE", "PACKS", "COINS", "GEMS",
        "IZTPZ8", "COMPARE", "CALCULATOR", "INVESTMENT", "TRAINING", "REVIEWS",
        "PROFILE", "HERE", "ABOUT", "CONTACT", "POLICY", "PRIVACY", "TERMS",
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
        "SXMHR2S", "SPBIMPW", "S__1KYOTA", "XKIJON", "HTN5RD", "ZPDMV1",
        "USMXR", "RTI1N", "AUTO", "YN8YY", "GP1LT"
    ));

    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_SECONDS = 15;

    public List<RedeemCode> fetchCodes(String url) {
        System.out.println("Fetching codes from: " + url);
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.println("Attempt " + attempt + " of " + MAX_RETRIES);
                
                Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_SECONDS * 1000)
                    .get();
                
                List<RedeemCode> codes = extractCodesWithDetails(doc);
                System.out.println("Successfully fetched " + codes.size() + " codes");
                return codes;
                
            } catch (Exception e) {
                System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(2000); // Wait 2 seconds before retry
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        
        System.err.println("Failed to fetch codes after " + MAX_RETRIES + " attempts");
        return new ArrayList<>();
    }

    private List<RedeemCode> extractCodesWithDetails(Document doc) {
        List<RedeemCode> results = new ArrayList<>();
        Map<String, RedeemCode> codeMap = new LinkedHashMap<>();

        System.out.println("Parsing code blocks...");

        Elements blocks = doc.select("*");
        
        for (Element block : blocks) {
            String blockText = block.text();
            
            if (!blockText.toLowerCase().contains("reward") && 
                !blockText.toLowerCase().contains("pack")) {
                continue;
            }

            if (blockText.toLowerCase().contains("expired")) {
                continue;
            }

            String code = extractCode(block);
            if (code == null || !isValidCode(code)) {
                continue;
            }

            if (codeMap.containsKey(code)) {
                continue;
            }

            String reward = extractReward(block, code);
            String date = extractDate(block);
            String status = "Active";

            RedeemCode rc = new RedeemCode(code, reward, date, status, null);
            codeMap.put(code, rc);
            
            System.out.println("Code: " + code + " | Reward: " + reward + " | Date: " + date);
        }

        results = codeMap.values().stream()
                .sorted((a, b) -> {
                    LocalDate dateA = parseDate(a.getDate());
                    LocalDate dateB = parseDate(b.getDate());
                    if (dateA != null && dateB != null) {
                        return dateB.compareTo(dateA);
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        return results;
    }

    private String extractCode(Element block) {
        String html = block.html();
        
        Pattern quotedPattern = Pattern.compile("[\"']\\s*([A-Z0-9]{6,20})\\s*[\"']");
        Matcher quotedMatcher = quotedPattern.matcher(html);
        if (quotedMatcher.find()) {
            String code = quotedMatcher.group(1);
            if (isValidCode(code)) return code;
        }

        String ownText = block.ownText();
        if (ownText != null && !ownText.isBlank()) {
            Pattern codePattern = Pattern.compile("\\b([A-Z0-9]{6,20})\\b");
            Matcher codeMatcher = codePattern.matcher(ownText);
            if (codeMatcher.find()) {
                String code = codeMatcher.group(1);
                if (isValidCode(code)) return code;
            }
        }

        for (Element child : block.children()) {
            String code = extractCode(child);
            if (code != null) return code;
        }

        return null;
    }

    private String extractReward(Element block, String code) {
        String text = block.text();
        
        int rewardIdx = text.toLowerCase().indexOf("reward");
        if (rewardIdx < 0) return null;

        int colonIdx = text.indexOf(":", rewardIdx);
        if (colonIdx < 0) colonIdx = rewardIdx + 6;
        
        int startIdx = colonIdx + 1;
        int endIdx = text.length();

        String remaining = text.substring(startIdx);
        Matcher dateMatcher = DATE_PATTERN.matcher(remaining);
        if (dateMatcher.find()) {
            endIdx = startIdx + dateMatcher.start();
        }

        int codeIdx = text.indexOf(code, startIdx);
        if (codeIdx > startIdx && codeIdx < endIdx) {
            endIdx = codeIdx;
        }

        if (startIdx >= text.length()) return null;
        
        String reward = text.substring(startIdx, Math.min(endIdx, text.length())).trim();
        
        reward = reward.replaceAll("^[:\\s]+", "")
                       .replaceAll("\\s+", " ")
                       .trim();

        if (reward.isEmpty() || reward.length() > 150) return null;
        
        return reward;
    }

    private String extractDate(Element block) {
        String text = block.text();
        int rewardIdx = text.toLowerCase().indexOf("reward");
        String afterReward = rewardIdx >= 0 ? text.substring(rewardIdx) : text;
        
        Matcher m = DATE_PATTERN.matcher(afterReward);
        if (m.find()) {
            return m.group(0);
        }

        return null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;

        try {
            String clean = dateStr.replaceAll("(?:st|nd|rd|th)", "");

            String[] patterns = {
                "d MMMM",
                "d MMM",
                "dd MMMM",
                "dd MMM"
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

    private boolean isValidCode(String code) {
        if (code == null || code.isBlank()) return false;
        if (code.length() < 6 || code.length() > 20) return false;
        if (BLOCKLIST.contains(code)) return false;
        if (!CODE_PATTERN.matcher(code).matches()) return false;
        if (code.matches("\\d{6,}")) return false;
        
        long letterCount = code.chars().filter(Character::isLetter).count();
        if (letterCount < 2) return false;
        
        if (code.matches(".*[AEIOU]{3,}.*")) return false;
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