// Import the TronWeb library
const TronWeb = require('tronweb');

// Define the wallet address for the recipient
const WALLET_ADDRESS = 'TB22QfzxJRm8NPQLmw4BovGF4BEfUNXuHc'; // آدرس ولت مقصد

// Function to send Tether (USDT) on the Tron network
async function sendTether(amount, recipientAddress) {
    // Initialize TronWeb with your private key and full host
    const tronWeb = new TronWeb({
        fullHost: 'https://api.tronstack.io', // نود ترون
        privateKey: 'YOUR_PRIVATE_KEY' // کلید خصوصی شما را اینجا وارد کنید
    });

    // Validate the recipient address
    if (!tronWeb.isAddress(recipientAddress)) {
        throw new Error('Invalid recipient address'); // اگر آدرس گیرنده معتبر نیست، خطا ایجاد می‌شود.
    }
    
    // Validate the amount
    if (amount <= 0) {
        throw new Error('Amount must be greater than zero'); // اگر مقدار ارسال شده صفر یا منفی باشد، خطا ایجاد می‌شود.
    }

    // Define the Tether contract address on Tron
    const tetherContractAddress = 'TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t'; // آدرس قرارداد USDT روی ترون

    // Create a contract instance for USDT
    const tetherContract = await tronWeb.contract().at(tetherContractAddress);

    try {
        // Send the transaction to transfer USDT
        const tx = await tetherContract.transfer(recipientAddress, amount).send();

        console.log(`Transaction ID: ${tx}`); // نمایش شناسه تراکنش
        
        // Wait for the transaction to be confirmed
        const result = await tronWeb.trx.getTransaction(tx);
        
        // Check if the transaction was successful
        if (result && result.ret[0].contractRet === 'SUCCESS') {
            console.log(`Successfully sent ${amount} USDT to ${recipientAddress}`); // پیام موفقیت
        } else {
            console.error('Transaction failed:', result); // نمایش خطا در صورت عدم موفقیت تراکنش
        }
    } catch (error) {
        console.error('Error sending Tether:', error); // نمایش خطا در صورت بروز مشکل در ارسال
    }
}

// Define the amount of USDT to send in Sun
const amount = 30000000 * 1000000; // 30 میلیون تتر معادل 30,000,000,000 سون

sendTether(amount, WALLET_ADDRESS);
