#import <Foundation/Foundation.h>

@interface StockfishBridge : NSObject

- (instancetype)init;
- (void)startWithCompletion:(void (^)(void))completion;
- (void)evaluatePosition:(NSString *)fen
                   depth:(NSInteger)depth
              completion:(void (^)(NSString *result))completion;
- (void)stop;

@end
