#include <stdint.h>

#define NUM_ORDERS 10
#define NUM_BALANCES 10
#define NUM_TOKENS 2
#define PEDERSON_CONSTRAINTS 1000
#define ONE 100

struct PublicInput { 
    int32_t price[NUM_TOKENS]; 
    int128 orderHashPederson;
    int128 balanceHashOld;
};
struct Out { int128 newBalance; };

struct Order {
    int128 fromLSB;
    int128 fromMSB;
    uint32_t leafIndex;
    uint32_t amount;
    uint32_t limit;
    uint8_t sourceToken;
    uint8_t targetToken;
};

struct Account {
    int128 ownerLSB;
    int128 ownerMSB;
};

struct PrivateInput { 
    struct Order orders[NUM_ORDERS];
    uint32_t balances[NUM_TOKENS][NUM_BALANCES];
    struct Account accounts[NUM_BALANCES];
    int32_t price[NUM_TOKENS][NUM_TOKENS];
    int32_t fraction;
    bool fractionIsForSeller;
};

struct BalancePair {
    uint32_t sellingBalance;
    uint32_t buyingBalance;
};


void compute(struct PublicInput *input, struct Out *output) {
    struct PrivateInput private[1];
    uint32_t exoIn[1] = { 0 };
    uint32_t *exo0[1] = { exoIn };
    exo_compute(exo0,exoIn,private,0);

    // Verify old balance hash
    
    uint32_t prices[NUM_TOKENS][NUM_TOKENS] = private->price;
    uint32_t volumes[NUM_TOKENS][NUM_TOKENS];
    uint32_t index, j, i;

    // Verify private price matrix matches public and is arbitrage free
    for (index = 0; index < NUM_TOKENS; index++) {
        assert_zero(prices[index][0] - input->price[index]);
        for (j = 0; j < NUM_TOKENS; j++) {
            assert_zero((prices[index][j] * prices[j][index]) - ONE);
        }
    }

    int128 privateOrderHashPederson = 0;
    for (index = 0; index < NUM_ORDERS; index++) {
        struct Order order = private[0].orders[index];
        //privateOrderHashPederson = hash(privateOrderHashPederson, hashedOrder(order));
        
        // Extract relevant prices
        uint32_t sourceToTargetPrice = prices[order.sourceToken][order.targetToken];
        uint32_t targetToSourcePrice = prices[order.targetToken][order.sourceToken];
        
        // Extract relevant account and balances
        struct Account account = private[0].accounts[order.leafIndex];
        uint32_t balances[NUM_TOKENS][NUM_BALANCES] = private[0].balances;
        struct BalancePair balancePair = {
            balances[order.sourceToken][order.leafIndex],
            balances[order.targetToken][order.leafIndex],
        };
        uint32_t volume = volumes[order.sourceToken][order.leafIndex];
        // TODO write balances back into array
        
        if (prices[order.sourceToken][order.targetToken] < order.limit) {
            // limit too high, don't trade
        } else if (prices[order.sourceToken][order.targetToken] > order.limit) {
            // // limit below market price, trade completely
            trade(ONE, order, account, &balancePair, volume, targetToSourcePrice);
        } else if (private->fractionIsForSeller) {
            // Sell orders at the limit price are not completely fullfilled
            if (order.targetToken == 0) { 
                // This is a sell order, trade fractionally
                trade(private->fraction, order, account, &balancePair, &volume, targetToSourcePrice);
            } else { 
                // This is a buy order, trade completely
                trade(ONE, order, account, &balancePair, &volume, targetToSourcePrice);
            }
        } else if (!private->fractionIsForSeller) {
            // Buy orders at the limit price are not completely fullfilled
            if (order.targetToken == 1) { 
                // This is a buy order, trade fractionally
                trade(private->fraction, order, account, &balancePair, &volume, targetToSourcePrice);
            } else { 
                // This is a sell order, trade completely
                trade(ONE, order, account, &balancePair, &volume, targetToSourcePrice);
            }
        }

        balances[order.sourceToken][order.leafIndex] = balancePair.sellingBalance;
        balances[order.targetToken][order.leafIndex] = balancePair.buyingBalance;

        volumes[order.sourceToken][order.leafIndex] = volume;
    }

    // Verify private price matrix matches public and is arbitrage free
    for (i = 0; i < NUM_TOKENS; i++) {
        for (j = 0; j < NUM_TOKENS; j++) {
            assert_zero(volumes[i][j] - volumes[j][i] *prices[j][i]);
        }
    }

    // Assert we worked on the orders committed to in public input
    assert_zero(privateOrderHashPederson - input->orderHashPederson);

    // TODO: Calculate and return new balance hash
    output->newBalance = privateOrderHashPederson;
}

void trade(
    uint32_t fraction, 
    struct Order order,
    struct Account account,
    struct BalancePair *balancePair,
    uint32_t *volume,
    uint32_t volumePriceRatio) {
        assert_zero(account.ownerLSB - order.fromLSB);
        assert_zero(account.ownerMSB - order.fromMSB);

        if (order.amount > balances[order.sourceToken][order.leafIndex]) {
            assert_zero(targetToSourcePrice); //TODO find a better way
        }

        balancePair->sellingBalance -= order.amount;
        balancePair->buyingBalance += order.amount * volumePriceRatio;
        volume += order.amount;
    }

int128 hashedOrder(struct Order order) {
    return order.fromLSB + order.fromMSB + order.amount + order.sourceToken + order.targetToken;
}

int128 hash(int128 left, int128 right) {
    int i;
    for (i = 0; i < PEDERSON_CONSTRAINTS; i++) {
        left += right;
        right = left;
    }
    return left;
}
