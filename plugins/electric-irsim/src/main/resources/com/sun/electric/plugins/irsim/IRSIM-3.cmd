stepsize 0.05

debug ev tau dc

||| define vectors. We'll load data by directly stimulating data path, 
||| rather than using scan chain
vector scan s[1] s[2] s[3] s[4] s[5]

vector dataina dataa[0] dataa[1] dataa[2] dataa[3] dataa[4] dataa[5] dataa[6] dataa[7] dataa[8] dataa[9] dataa[10] dataa[11] dataa[12] dataa[13] dataa[14] dataa[15] dataa[16] dataa[17] dataa[18] dataa[19] dataa[20] dataa[21] dataa[22] dataa[23] dataa[24] dataa[25] dataa[26] dataa[27] dataa[28] dataa[29] dataa[30] dataa[31] dataa[32] dataa[33] dataa[34] dataa[35] dataa[36]

vector datainc datac[0] datac[1] datac[2] datac[3] datac[4] datac[5] datac[6] datac[7] datac[8] datac[9] datac[10] datac[11] datac[12] datac[13] datac[14] datac[15] datac[16] datac[17] datac[18] datac[19] datac[20] datac[21] datac[22] datac[23] datac[24] datac[25] datac[26] datac[27] datac[28] datac[29] datac[30] datac[31] datac[32] datac[33] datac[34] datac[35] datac[36]

||| plot relevant nodes
ana scan
ana dataina
ana send[a]
ana out[a][0]
ana stop[a]

||| initialization
set scan 00001
h s[5]
h send[a]
h send[c]
h out[a][0]
h out[c][0]
h stop[a]
h stop[c]
l doit[b]
l doit[d]
set dataina 0000000000000000000000000000000000000
set datainc 0000000000000000000000000000000000000
s 0.5
h */go
s 0.5
x */go
set scan 00000
s 5
l s[5]
s 0.5

set dataina 1000000000000000000000000000000000001
s 0.2
set dataina XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
s
l stop[a]
s
s 5
				
