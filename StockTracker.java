import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import java.util.Scanner;
import java.text.DecimalFormat;

public class StockTracker {
    private static final String API_KEY = "KFPE0YVTE3GSMXDJ";
    private static final String QUOTE_URL = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE";
    private static final String HISTORICAL_URL = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY";
    private static final DecimalFormat df = new DecimalFormat("0.00");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Stock Price Tracker with Recommendations");
        System.out.println("---------------------------------------");
        
        while (true) {
            System.out.print("Enter stock symbol (e.g., AAPL, MSFT, RELIANCE.BSE) or 'quit' to exit: ");
            String symbol = scanner.nextLine().trim().toUpperCase();
            
            if (symbol.equalsIgnoreCase("QUIT")) {
                break;
            }
            
            try {
                // Get current price data
                JSONObject quoteData = getQuoteData(symbol);
                String currentPrice = quoteData.getJSONObject("Global Quote").getString("05. price");
                long volume = Long.parseLong(quoteData.getJSONObject("Global Quote").getString("06. volume"));
                
                System.out.println("\nCurrent price of " + symbol + ": " + cleanPrice(currentPrice));
                
                // Get historical data
                double[] lastTwoPrices = getHistoricalPrices(symbol);
                printAsciiGraph(lastTwoPrices[1], lastTwoPrices[0]); // current, previous
                
                // Generate recommendation and show trading activity
                generateRecommendation(lastTwoPrices[1], lastTwoPrices[0], volume);
                
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                System.out.println("Note: For Indian stocks, use format like RELIANCE.BSE or TATASTEEL.NS");
            }
            
            // Add delay to avoid API rate limits
            try {
                Thread.sleep(12000); // Wait 12 seconds between calls
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        scanner.close();
        System.out.println("Thank you for using Stock Price Tracker!");
    }

    // Fetches current quote data
    private static JSONObject getQuoteData(String symbol) throws Exception {
        String urlString = QUOTE_URL + "&symbol=" + symbol + "&apikey=" + API_KEY;
        JSONObject json = fetchApiData(urlString);
        
        if (json.has("Error Message")) {
            throw new RuntimeException(json.getString("Error Message"));
        }
        if (!json.has("Global Quote")) {
            throw new RuntimeException("No data available for symbol: " + symbol);
        }
        
        return json;
    }

    // Fetches historical prices (returns [previousClose, currentClose])
    private static double[] getHistoricalPrices(String symbol) throws Exception {
        String urlString = HISTORICAL_URL + "&symbol=" + symbol + "&apikey=" + API_KEY + "&outputsize=compact";
        JSONObject json = fetchApiData(urlString);
        
        if (json.has("Error Message")) {
            throw new RuntimeException(json.getString("Error Message"));
        }
        
        JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");
        String[] dates = timeSeries.keySet().toArray(new String[0]);
        
        // Get most recent two days
        String currentDate = dates[0];
        String previousDate = dates[1];
        
        double currentClose = Double.parseDouble(
            timeSeries.getJSONObject(currentDate).getString("4. close"));
        double previousClose = Double.parseDouble(
            timeSeries.getJSONObject(previousDate).getString("4. close"));
        
        return new double[]{previousClose, currentClose};
    }

    // Helper to fetch API data
    private static JSONObject fetchApiData(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return new JSONObject(response.toString());
    }

    // ASCII Graph Generator
    private static void printAsciiGraph(double current, double previous) {
        // Calculate percentage change
        double changePercent = ((current - previous) / previous) * 100;
        String changeString = String.format("%.1f%%", changePercent);
        
        // Scale for graph (1 bar = $10)
        int currentBars = (int) (current / 10);
        int previousBars = (int) (previous / 10);
        
        System.out.println("\nPrice Trend Graph");
        System.out.println("──────────────────────────────────────");
        System.out.println("Today: " + "█".repeat(currentBars) + " $" + String.format("%.2f", current));
        System.out.println("Prev:  " + "█".repeat(previousBars) + " $" + String.format("%.2f", previous) + 
                         " (" + (changePercent >= 0 ? "+" : "") + changeString + ")");
    }

    // Generate trading recommendation
    private static void generateRecommendation(double currentPrice, double previousClose, long volume) {
        double changePercent = ((currentPrice - previousClose) / previousClose) * 100;
        String trend = changePercent >= 0 ? "▲" : "▼";
        
        // Generate recommendation
        String recommendation;
        if (changePercent > 5 && volume > 1_000_000) {
            recommendation = "STRONG BUY (High upward momentum)";
        } else if (changePercent > 2) {
            recommendation = "BUY (Positive trend)";
        } else if (changePercent < -5 && volume > 1_000_000) {
            recommendation = "STRONG SELL (Heavy selling)";
        } else if (changePercent < -2) {
            recommendation = "SELL (Downward trend)";
        } else {
            recommendation = "HOLD (Neutral movement)";
        }
        
        // Display recommendation
        System.out.println("\n=== Trading Recommendation ===");
        System.out.println("Price Change: " + trend + " " + df.format(Math.abs(changePercent)) + "%");
        System.out.println("Volume: " + formatVolume(volume));
        System.out.println("Action: " + recommendation);
        
        // Visualize market sentiment
        visualizeMarketSentiment(changePercent, volume);
    }

    // Visualize buying/selling pressure
    private static void visualizeMarketSentiment(double changePercent, long volume) {
        System.out.println("\nMarket Sentiment Analysis");
        System.out.println("──────────────────────────────────────");
        
        // Calculate sentiment proportions (simplified example)
        int buyWidth = (int) (10 * (1 + changePercent/20));
        int sellWidth = (int) (10 * (1 - changePercent/20));
        buyWidth = Math.max(1, Math.min(buyWidth, 9));
        sellWidth = Math.max(1, Math.min(sellWidth, 9));
        int holdWidth = 10 - buyWidth - sellWidth;
        
        // Adjust for volume
        if (volume > 2_000_000) {
            buyWidth = (int)(buyWidth * 1.2);
            sellWidth = (int)(sellWidth * 1.2);
        }
        
        System.out.println("Buy Pressure:  " + "█".repeat(buyWidth) + " " + (buyWidth*10) + "%");
        System.out.println("Hold Position: " + "█".repeat(holdWidth) + " " + (holdWidth*10) + "%");
        System.out.println("Sell Pressure: " + "█".repeat(sellWidth) + " " + (sellWidth*10) + "%");
    }

    // Format volume for display
    private static String formatVolume(long volume) {
        if (volume >= 1_000_000) {
            return String.format("%.1fM", volume / 1_000_000.0);
        } else if (volume >= 1_000) {
            return String.format("%.1fK", volume / 1_000.0);
        }
        return Long.toString(volume);
    }

    // Cleans price string
    private static String cleanPrice(String price) {
        return price.replaceAll("[^\\x00-\\x7F]", "");
    }
}