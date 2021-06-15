#include "./3sBid/sBid.h"
#include "./3server/server.h"
extern ZZ sk_debug;
extern bool debug;
int main(int argc, char** argv)
{
	int op, ch;
	if (argc == 1)
	{
		if (debug) {
			cout << "Input your code:" << flush;
			array<int, 6> codes;
			cin >> codes[0];
			cout << "Input your opponent's code:" << flush;
			cin >> codes[1];
			cout << "Input round:" << flush;
			cin >> codes[2];
			SBid sbid;
			sbid.prepare(codes);
			sbid.bid();
			sbid.verify();
		}
		else {
			cout << "please input \"./sBid -h\" to learn more" << endl;
		}
	}
	else {
		while ((ch = getopt(argc, argv, "b:v:d:r:g")) != -1)
		{
			switch (ch)
			{
				case 'b': {
					array<int, 6> codes;
					codes[0] = atoi(optarg);//自己的index
					codes[1] = atoi(argv[optind]);//对方的index
					codes[2] = atoi(argv[optind + 1]);//本轮的round
					codes[3] = atoi(argv[optind + 2]);//自己的lastFinishRound
					codes[4] = atoi(argv[optind + 3]);//对方的lastFinishRound
					codes[5] = atoi(argv[optind + 4]);//0:价低胜 1：价高胜
					string outFile = optarg;
					string errFile = outFile + "_err.log";
					outFile += "_out.log";
					freopen(outFile.c_str(), "a", stdout);
					freopen(errFile.c_str(), "a", stderr);
					setbuf(stdout, NULL);
					SBid sbid;
					sbid.prepare(codes);
					time_t rawtime;
					struct tm* info;
					char buffer[80];
					time(&rawtime);
					info = localtime(&rawtime);
					strftime(buffer, 80, "%Y-%m-%d %H:%M:%S", info);
					cout << "[" << codes[0] << "] - " << "Start at " << buffer << endl;
					clock_t begin = GetTickCount();
					sbid.bid();
					sbid.verify();
					clock_t end = GetTickCount();
					double cTime = (end - begin) / (double)CLOCKS_PER_SEC * 1000;
					time(&rawtime);
					info = localtime(&rawtime);
					strftime(buffer, 80, "%Y-%m-%d %H:%M:%S", info);
					cout << "[" << codes[0] << "] - " << "End at " << buffer << endl;
					cout << "[" << codes[0] << "] - " << "Total time " << cTime << " ms\n" << endl;
					fclose(stdout);
					fclose(stderr);
					break;
				}
				case 'v': {
					//Verify
					array<string, 3> codes;
					codes[0] = optarg;//index
					codes[1] = argv[optind];//round
					codes[2] = argv[optind + 1];//上一轮竞标的round
					string outFile = optarg;
					outFile += "_verify.log";
					freopen(outFile.c_str(), "a", stdout);
					setbuf(stdout, NULL);
					time_t rawtime;
					struct tm* info;
					char buffer[80];
					time(&rawtime);
					info = localtime(&rawtime);
					strftime(buffer, 80, "%Y-%m-%d %H:%M:%S", info);
					cout << "==========[" << buffer << "]==========" << endl;
					vMode = true;
					SBid sbid;
					/*sbid.prepare(codes);
					sbid.verify();*/
					sbid.unzip(codes);
					sbid.verify();
					time(&rawtime);
					info = localtime(&rawtime);
					strftime(buffer, 80, "%Y-%m-%d %H:%M:%S", info);
					cout << "==========[" << buffer << "]==========" << endl;
					fclose(stdout);
					break;
				}
				case 'g': {//parameters gen
					string outFile = "parameters.log";
					string errFile = "parameters_err.log";
					freopen(outFile.c_str(), "a", stdout);
					freopen(errFile.c_str(), "a", stderr);
					setbuf(stdout, NULL);
					SBid sbid;
					sbid.parametersGen();
					fclose(stdout);
					fclose(stderr);
					break;
				}
				case 'r': {
					SBid sbid;
					string outFile = optarg;
					outFile += "_out.log";
					freopen(outFile.c_str(), "a", stdout);
					setbuf(stdout, NULL);
					sbid.registration(optarg);
					fclose(stdout);
					break;
				}
				case 'd': {
					array<string, 3> paras;
					paras[0] = optarg;//cipherAmount
					paras[1] = argv[optind];//sk
					paras[2] = argv[optind + 1];//mod
					string outFile = "decrypt.log";
					string errFile = "decrypt_err.log";
					freopen(outFile.c_str(), "a", stdout);
					freopen(errFile.c_str(), "a", stderr);
					setbuf(stdout, NULL);
					SBid sbid;
					sbid.decrypt(paras);
					fclose(stdout);
					fclose(stderr);
					break;
				}
				case '?': {
					printf("unknow option:%c\n", optopt);
					break;
				}
				default: {
					cout << "./sBid -g : Parameter Generation" << endl;
					cout << "./sBid -b : Bidding function" << endl;
					cout << "./sBid -v : Verification function" << endl;
					break;
				}
			}
		}
	}
	return 0;
}