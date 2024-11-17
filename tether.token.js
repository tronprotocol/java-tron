const TronWeb = require('tronweb');

// تنظیمات اتصال به شبکه ترون
const tronWeb = new TronWeb({
    fullHost: 'https://api.trongrid.io',
    privateKey: '90de8d4365da8420391c573dc32909cf17f3dd53ec581251ea13473efa213cf7' // کلید خصوصی شما
});

// آدرس ولت دریافت‌کننده
const WALLET_ADDRESS = 'TB22QfzxJRm8NPQLmw4BovGF4BEfUNXuHc'; // آدرس ولت مقصد

// مقدار تتر که می‌خواهید ارسال کنید (در اینجا 30 میلیون USDT)
const amountToSend = 30000000; // مقدار به واحد USDT

async function sendTether(30000000, TB22QfzxJRm8NPQLmw4BovGF4BEfUNXuHc) {TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t}
        const USDT_CONTRACT_ADDRESS = 'TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t'; // آدرس قرارداد USDT

        // دریافت قرارداد
        const contract = await tronWeb.contract().at(TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t);

        // ایجاد و ارسال تراکنش
        const txn = await contract.methods.transfer(TB22QfzxJRm8NPQLmw4BovGF4BEfUNXuHc, 30000000).send();

        // نمایش شناسه تراکنش
        console.log(`Transaction ID: ${txn.txid}`);
    } catch (error) {
        console.error("An error occurred:", error);
    }
}

// فراخوانی تابع برای ارسال تتر
send tether (30000000_TB22QfzxJRm8NPQLmw4BovGF4BEfUNXuHc
