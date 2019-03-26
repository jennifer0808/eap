****************************************************************************
*  Filename    : TowaHost.sml
*  Description : Pfile for the HOST side of the towa
*  Author      : luosy
*  Date        : 05/25/2016
*
****************************************************************************
* Find out MAX_SVIDREQ, MAX_IMAGES & MAX_COMPARM values
#define MAX_STRIP_MAP_DATA_LENGTH   12000
#define MAX_VARIDS      50
#define MAX_COMPARM     30

****************************************************************************
* S?F0    Abort Transaction                                    E <-> H     *
****************************************************************************

S1F0 = s1f0out OUTPUT.

S1F0 = s1f0in INPUT.

S9F0 =s9f0out OUTPUT.

S9F0 =s9f0in INPUT.

****************************************************************************
* S1F1    Are You There/Online? (R)                            H <-> E     *
****************************************************************************

S1F1 = s1f1in INPUT W.

S1F1 = s1f1out OUTPUT W.

****************************************************************************
* S1F2    On Line Data (D)                                     E <-> H     *
****************************************************************************

S1F2 =s1f2out OUTPUT
  <L [0]>.

S1F2 =s1f2in INPUT
  <L [2]
      <A [MAX 6] >               = Mdln
      <A [MAX 6] >               = SoftRev
  >.
****************************************************************************
* S1F3    Select Equipment Status Request                      E <- H     *
****************************************************************************
S1F3 =s1f3out OUTPUT W
  <L [0]>.

S1F3 =s1f3singleout OUTPUT W
<L [1]
    <U2 [1]> = SVID
>.

S1F3 = s1f3statecheck OUTPUT W
 <L [3]
    <U2 [1]> = EquipStatus
    <U2 [1]> = PPExecName
    <U2 [1]> = ControlState
>.

S1F3 = s1f3rcpandstate OUTPUT W
 <L [3]
    <U2 [1]>=EquipStatus
    <U2 [1]>=PreProcessStatus
    <U2 [1]>=PPExecName
>.

S1F3 = s1f3svshot OUTPUT W
 <L [4]
    <U2 [1]>=Data0
    <U2 [1]>=Data1
    <U2 [1]>=Data2
    <U2 [1]>=Data3
>.

S1F3 = s1f3pressout OUTPUT W
<L [4]
    <U2 [1]> = Press1
    <U2 [1]> = Press2
    <U2 [1]> = Press3
    <U2 [1]> = Press4
>.

S1F3 = s1f3Specific OUTPUT W
<V>=DATA.

S1F3 =s1f3TOWAY1RRcpPara OUTPUT W
<L[65]
<U2 [1]>=Data0
<U2 [1]>=Data1
<U2 [1]>=Data2
<U2 [1]>=Data3
<U2 [1]>=Data4
<U2 [1]>=Data5
<U2 [1]>=Data6
<U2 [1]>=Data7
<U2 [1]>=Data8
<U2 [1]>=Data9
<U2 [1]>=Data10
<U2 [1]>=Data11
<U2 [1]>=Data12
<U2 [1]>=Data13
<U2 [1]>=Data14
<U2 [1]>=Data15
<U2 [1]>=Data16
<U2 [1]>=Data17
<U2 [1]>=Data18
<U2 [1]>=Data19
<U2 [1]>=Data20
<U2 [1]>=Data21
<U2 [1]>=Data22
<U2 [1]>=Data23
<U2 [1]>=Data24
<U2 [1]>=Data25
<U2 [1]>=Data26
<U2 [1]>=Data27
<U2 [1]>=Data28
<U2 [1]>=Data29
<U2 [1]>=Data30
<U2 [1]>=Data31
<U2 [1]>=Data32
<U2 [1]>=Data33
<U2 [1]>=Data34
<U2 [1]>=Data35
<U2 [1]>=Data36
<U2 [1]>=Data37
<U2 [1]>=Data38
<U2 [1]>=Data39
<U2 [1]>=Data40
<U2 [1]>=Data41
<U2 [1]>=Data42
<U2 [1]>=Data43
<U2 [1]>=Data44
<U2 [1]>=Data45
<U2 [1]>=Data46
<U2 [1]>=Data47
<U2 [1]>=Data48
<U2 [1]>=Data49
<U2 [1]>=Data50
<U2 [1]>=Data51
<U2 [1]>=Data52
<U2 [1]>=Data53
<U2 [1]>=Data54
<U2 [1]>=Data55
<U2 [1]>=Data56
<U2 [1]>=Data57
<U2 [1]>=Data58
<U2 [1]>=Data59
<U2 [1]>=Data60
<U2 [1]>=Data61
<U2 [1]>=Data62
<U2 [1]>=Data63
<U2 [1]>=Data64
>.

S1F3 =s1f3TOWAY1ERcpPara OUTPUT W
<L[64]
<U2 [1]>=Data0
<U2 [1]>=Data1
<U2 [1]>=Data2
<U2 [1]>=Data3
<U2 [1]>=Data4
<U2 [1]>=Data5
<U2 [1]>=Data6
<U2 [1]>=Data7
<U2 [1]>=Data8
<U2 [1]>=Data9
<U2 [1]>=Data10
<U2 [1]>=Data11
<U2 [1]>=Data12
<U2 [1]>=Data13
<U2 [1]>=Data14
<U2 [1]>=Data15
<U2 [1]>=Data16
<U2 [1]>=Data17
<U2 [1]>=Data18
<U2 [1]>=Data19
<U2 [1]>=Data20
<U2 [1]>=Data21
<U2 [1]>=Data22
<U2 [1]>=Data23
<U2 [1]>=Data24
<U2 [1]>=Data25
<U2 [1]>=Data26
<U2 [1]>=Data27
<U2 [1]>=Data28
<U2 [1]>=Data29
<U2 [1]>=Data30
<U2 [1]>=Data31
<U2 [1]>=Data32
<U2 [1]>=Data33
<U2 [1]>=Data34
<U2 [1]>=Data35
<U2 [1]>=Data36
<U2 [1]>=Data37
<U2 [1]>=Data38
<U2 [1]>=Data39
<U2 [1]>=Data40
<U2 [1]>=Data41
<U2 [1]>=Data42
<U2 [1]>=Data43
<U2 [1]>=Data44
<U2 [1]>=Data45
<U2 [1]>=Data46
<U2 [1]>=Data47
<U2 [1]>=Data48
<U2 [1]>=Data49
<U2 [1]>=Data50
<U2 [1]>=Data51
<U2 [1]>=Data52
<U2 [1]>=Data53
<U2 [1]>=Data54
<U2 [1]>=Data55
<U2 [1]>=Data56
<U2 [1]>=Data57
<U2 [1]>=Data58
<U2 [1]>=Data59
<U2 [1]>=Data60
<U2 [1]>=Data61
<U2 [1]>=Data62
<U2 [1]>=Data63
>.

S1F3 =s1f3TOWAPMCRcpPara OUTPUT W
<L[80]
<U2 [1]>=Data0
<U2 [1]>=Data1
<U2 [1]>=Data2
<U2 [1]>=Data3
<U2 [1]>=Data4
<U2 [1]>=Data5
<U2 [1]>=Data6
<U2 [1]>=Data7
<U2 [1]>=Data8
<U2 [1]>=Data9
<U2 [1]>=Data10
<U2 [1]>=Data11
<U2 [1]>=Data12
<U2 [1]>=Data13
<U2 [1]>=Data14
<U2 [1]>=Data15
<U2 [1]>=Data16
<U2 [1]>=Data17
<U2 [1]>=Data18
<U2 [1]>=Data19
<U2 [1]>=Data20
<U2 [1]>=Data21
<U2 [1]>=Data22
<U2 [1]>=Data23
<U2 [1]>=Data24
<U2 [1]>=Data25
<U2 [1]>=Data26
<U2 [1]>=Data27
<U2 [1]>=Data28
<U2 [1]>=Data29
<U2 [1]>=Data30
<U2 [1]>=Data31
<U2 [1]>=Data32
<U2 [1]>=Data33
<U2 [1]>=Data34
<U2 [1]>=Data35
<U2 [1]>=Data36
<U2 [1]>=Data37
<U2 [1]>=Data38
<U2 [1]>=Data39
<U2 [1]>=Data40
<U2 [1]>=Data41
<U2 [1]>=Data42
<U2 [1]>=Data43
<U2 [1]>=Data44
<U2 [1]>=Data45
<U2 [1]>=Data46
<U2 [1]>=Data47
<U2 [1]>=Data48
<U2 [1]>=Data49
<U2 [1]>=Data50
<U2 [1]>=Data51
<U2 [1]>=Data52
<U2 [1]>=Data53
<U2 [1]>=Data54
<U2 [1]>=Data55
<U2 [1]>=Data56
<U2 [1]>=Data57
<U2 [1]>=Data58
<U2 [1]>=Data59
<U2 [1]>=Data60
<U2 [1]>=Data61
<U2 [1]>=Data62
<U2 [1]>=Data63
<U2 [1]>=Data64
<U2 [1]>=Data65
<U2 [1]>=Data66
<U2 [1]>=Data67
<U2 [1]>=Data68
<U2 [1]>=Data69
<U2 [1]>=Data70
<U2 [1]>=Data71
<U2 [1]>=Data72
<U2 [1]>=Data73
<U2 [1]>=Data74
<U2 [1]>=Data75
<U2 [1]>=Data76
<U2 [1]>=Data77
<U2 [1]>=Data78
<U2 [1]>=Data79
>.

S1F3 =s1f3TOWAYPMRcpPara OUTPUT W
<L[61]
<U2 [1]>=Data0
<U2 [1]>=Data1
<U2 [1]>=Data2
<U2 [1]>=Data3
<U2 [1]>=Data4
<U2 [1]>=Data5
<U2 [1]>=Data6
<U2 [1]>=Data7
<U2 [1]>=Data8
<U2 [1]>=Data9
<U2 [1]>=Data10
<U2 [1]>=Data11
<U2 [1]>=Data12
<U2 [1]>=Data13
<U2 [1]>=Data14
<U2 [1]>=Data15
<U2 [1]>=Data16
<U2 [1]>=Data17
<U2 [1]>=Data18
<U2 [1]>=Data19
<U2 [1]>=Data20
<U2 [1]>=Data21
<U2 [1]>=Data22
<U2 [1]>=Data23
<U2 [1]>=Data24
<U2 [1]>=Data25
<U2 [1]>=Data26
<U2 [1]>=Data27
<U2 [1]>=Data28
<U2 [1]>=Data29
<U2 [1]>=Data30
<U2 [1]>=Data31
<U2 [1]>=Data32
<U2 [1]>=Data33
<U2 [1]>=Data34
<U2 [1]>=Data35
<U2 [1]>=Data36
<U2 [1]>=Data37
<U2 [1]>=Data38
<U2 [1]>=Data39
<U2 [1]>=Data40
<U2 [1]>=Data41
<U2 [1]>=Data42
<U2 [1]>=Data43
<U2 [1]>=Data44
<U2 [1]>=Data45
<U2 [1]>=Data46
<U2 [1]>=Data47
<U2 [1]>=Data48
<U2 [1]>=Data49
<U2 [1]>=Data50
<U2 [1]>=Data51
<U2 [1]>=Data52
<U2 [1]>=Data53
<U2 [1]>=Data54
<U2 [1]>=Data55
<U2 [1]>=Data56
<U2 [1]>=Data57
<U2 [1]>=Data58
<U2 [1]>=Data59
<U2 [1]>=Data60
>.

S1F3 =s1f3OutMulti OUTPUT W
<V> = SVList
.
****************************************************************************
* S1F4    Selected Equipment Status                       E -> H     *
****************************************************************************

S1F4 = s1f4statein INPUT 
  <L [3]  
    <U2 [1]> = EQSTATE
    <U1 [1]> = PPSTATE  
    <A [MAX 100]> = PPSTATENAME  
>.  
S1F4 = s1f4recipein INPUT 
  <L [6]
    <A [MAX 100]> = PPExecName
    <U1 [1]> = PPFormat
    <A [MAX 100]> = PPError
    <U4 [1]> = PPErrorList
    <A [MAX 100]> = PPWarn
    <U4 [1]> = PPWarnList
>.
*S1F4 = s1f4statecheck INPUT 
 * <L [2]  
  *  <U2 [1]> = EQSTATE
   * <A [MAX 100]> = PPExecName  
*>. 
S1F4 = s1f4in INPUT 
  <V> = RESULT.

****************************************************************************
* S1F11    Status Values Namelists Request  (svnr)              E <- H     *
****************************************************************************
S1F11 =s1f11out OUTPUT W
  <L [0]>.

****************************************************************************
* S1F12    Status Values Namelists Reply  (svnrr)                   E ->H  *
****************************************************************************
S1F12 =s1f12in INPUT 
       <V> = RESULT.

****************************************************************************
* S1F13   Establish Communications Request (CR)                H <-> E     *
****************************************************************************

S1F13 =s1f13in INPUT W
  <L [2]
    <A [MAX 6] >                     = Mdln
    <A [MAX 6] >                     = SoftRev
  >.
  
S1F13 = s1f13out OUTPUT W
  <L [0]>.

****************************************************************************
* S1F14   Establish Communications Request Acknowledge (CR)      H <-> E   *
****************************************************************************
  
S1F14 = s1f14in INPUT
  <L [2]
    <B [1] >                          = AckCode
    <L [2]
      <A [MAX 6] >                    = Mdln
      <A [MAX 6] >                    = SoftRev
    >
  >.

S1F14 = s1f14out OUTPUT
  <L [2]
     <B [1] >                     = AckCode
      <L [0] >
  >.
  
  ****************************************************************************
* S1F15   Request OFF-LINE                H -> E     *
****************************************************************************
S1F15 = s1f15out OUTPUT W.
 
****************************************************************************
* S1F16   OFF-LINE ACK                H <- E     *
****************************************************************************
S1F16 INPUT = s1f16in
<B [1]> = AckCode.
 
****************************************************************************
* S1F17   Request ON-LINE                H -> E     *
****************************************************************************
S1F17 = s1f17out OUTPUT W.

****************************************************************************
* S1F18   ON-LINE ACK                H <- E     *
****************************************************************************
S1F18 INPUT = s1f18in
<B [1]>=AckCode.
****************************************************************************
* S6F11  Event Report Send (ERS)                               E -> H      *
****************************************************************************

S6F11 =s6f11equipstate INPUT W 
<L [3]
	<U2 [1] > = DataID
	<U2 [1] > = CollEventID	
	<L [0]
	>
>.
S6F11 = s6f11equipstatuschange INPUT W 
<L [3]
	<U2[1] > = DataID
	<U2[1] > = CollEventID
	<L [1]
		<L [2]
			<U2 [1] > = ReportId
			<L [2]
                                <U2 [1] > = EquipStatus
				<A [MAX 100]> = PPExecName				
			>
		>
	>
>.
S6F11 = s6f11ppselectfinish INPUT W 
<L [3]
	<U2[1] > = DataID
	<U2[1] > = CollEventID
	<L [1]
		<L [2]
			<U2 [1] > = ReportId
			<L [1]                          
				<A [MAX 100]> = PPExecName				
			>
		>
	>
>.
S6F11 = s6f112dcodereview INPUT W 
<L [3]
	<U2[1] >   =  DataID
	<U2[1] >   =   CollEventID
	<L [1]
		<L [2]
			<U2[1] >   =   ReportId
			<L [5]
				<U2[1] >   =   PressNo
				<A[MAX 50]>   =   RecipeName
				<A[MAX 50]>   =   FormingNo
				<A[MAX 50]>   =   LeftStripID
				<A[MAX 50]>   =   RightStripID
			>
		>
	>
>.
****************************************************************************
* S6F12  Event Report Acknowledge (ERA)                        H -> E      *
****************************************************************************

S6F12 OUTPUT = s6f12out
  <B [1]>  = AckCode.

S6F12 Input = s6f12in
  <B [1]>     = AckCode.

****************************************************************************
* S10F1  Terminal request                                     E -> H      *
****************************************************************************
S10F1 input = s10f1in W
<L [2]
        <B[1]>      = TID
        <A [MAX 128]>   = TEXT
>.
S10F2 output = s10f2out
 <B [1]>        = AckCode
.

S10F3 output = s10f3out W
<L [2]
        <B[1]>      = TID
        <A [MAX 1000]>   = TEXT
>.
S10F4 input = s10f4in
 <B [1]>        = AckCode
.

****************************************************************************
* S14F1  GetAttr Request (GAR)                               H  <-  E      *
****************************************************************************
S14F1 INPUT = s14f1in W
<L [5]
    <A ''>
    <A 'Substrate'>
    <L [1]
      <A [MAX 80]>                  = StripId
    >
    <L [1]
      <L [3]
         <A 'SubstrateType'>
         <A 'Strip'>
         <U1 0>
      >
    > 
    <L [1]
        <A 'MapData'> 
    >
>.

****************************************************************************
* S14F2  GetAttr Data (GAD)                                   H <-> E      *
****************************************************************************
S14F2 OUTPUT = s14f2out
<L [2]
    <L [1]
      <L [2]
        <A [MAX 80]>              = StripId
        <L [1]
        	<L[2]
        	   <A  'MapData'> 
	           <A  [MAX MAX_STRIP_MAP_DATA_LENGTH]>    = MapData
	        >
	    >
	  >
	>
    <L [2]
      <U1 0>
      <L [0]>
    >
>.

S14F2 OUTPUT = s14f2outException
<L [2]
    <L [1]
      <L [2]
        <A [MAX 80]>              = StripId
        <L [1]
          <L [2]
             <A  'MapData'> 
	         <A  ''>
          >
        >
      >
    >
    <L [2]
      <U1 1> 
      <L [1]
        <L [2]
           <U1 [1]>              = ErrCode
           <A [MAX 80]>          = ErrText
        >
      >
    >
>.
 
****************************************************************************
* S9F1  Unrecognized Device Id (UDN)                           E -> H      *
****************************************************************************

S9F1 OUTPUT = s9f1out
  <B [10] >                      = BadDevHead.
 
S9F1 INPUT = s9f1input
  <B [10] >                      = BadDevHead.

****************************************************************************
* S9F3  Unrecognized Stream Type (USN)                         E -> H      *
****************************************************************************

S9F3 OUTPUT = s9f3out
  <B [10] >                      = BadStreamHead.

S9F3 INPUT = s9f3input
  <B [10] >                      = BadStreamHead.

****************************************************************************
* S9F5  Unrecognized Function Type (UFN)                       E -> H      *
****************************************************************************

S9F5 OUTPUT = s9f5out
  <B [10] >                      = BadFuncHead.

S9F5 INPUT = s9f5input
  <B [10] >                      = BadFuncHead.

****************************************************************************
* S9F7  Illegal Data (IDN)                                     E -> H      *
****************************************************************************

S9F7 OUTPUT = s9f7out
  <B [10] >                      = IllDataHead.

 S9F7 INPUT = s9f7input
  <B [10] >                      = IllDataHead.

****************************************************************************
* S9F9  Transaction Timer Timeout (TTN)                        E -> H      *
****************************************************************************

S9F9 OUTPUT = s9f9out
  <B [10] >                      = TranTOHead.

 S9F9 INPUT = s9f9input
  <B [10] >                      = TranTOHead.

****************************************************************************
* S9F11  Data Too Long (DLN)                                   E -> H      *
****************************************************************************

S9F11 OUTPUT = s9f11out
  <B [10] >                      = DataLongHead.

S9F11 INPUT = s9f11input
  <B [10] >                      = DataLongHead.
****************************************************************************
* S2F13    Equipment Constant Request                          H -> E      *
****************************************************************************

S2F13 =s2f13out OUTPUT W
< L [0]>.

S2F13 =s2f13singleout OUTPUT W
<L [1]
    <U2 [1]> = ECID
>.

****************************************************************************
* S2F14    Equipment Constant Data                          H<- E      *
****************************************************************************

S2F14 =s2f14in INPUT 
< V >=RESULT.

****************************************************************************
* S2F15   New Equipment Constant Send                          H-> E      *
****************************************************************************

S2F15 =s2f15out OUTPUT W
< L [1]
    < L [2]
        <U4 [1]> = ECID
        <F8 [1]> = ECV
    >
>.
****************************************************************************
* S2F16   New Equipment Constant ACK                         H<- E      *
****************************************************************************

S2F16 =s2f16in INPUT 
< L [1]
 < U4 [1]> = AckCode
>.
****************************************************************************
* S2F21   Remote Command Send                         H- >E      *
****************************************************************************

S2F21 =s2f21out OUTPUT  W
< L [1]
 < U4 [1]> = RCMD
>.
****************************************************************************
* S2F22   Remote Command ACK                        H- >E      *
****************************************************************************

S2F22 = s2f22in INPUT 
 < B [1]> = AckCode.
****************************************************************************
* S2F29   Equipment Canstant Namelist Request                   H- >E      *
****************************************************************************

S2F29 =s2f29out OUTPUT W 
< L [0]>.
S2F29 =s2f29oneout OUTPUT W 
< L [1]
    <U4[1]>=ECID
>.

****************************************************************************
* S2F30     Equipment Canstant Namelist Reply                    H- >E      *
****************************************************************************

S2F30 =s2f30in INPUT 
<V> = RESULT.

****************************************************************************
* S2F33    Define Report (DR)                                  H -> E      *
****************************************************************************

S2F33 OUTPUT = s2f33out W
<L [2]
    <U2 [1]>=DataID
    <L[1]
        <L [2]
            <U2 [1]>              =ReportID
            <L[1]
               <U2 [1]>          = VariableID            
            >
        >        
    >
>.
S2F33 OUTPUT = s2f33eqpstate W
<L [2]
    <U2 [1]>=DataID
    <L[1]
        <L [2]
            <U2 [1]>            =ReportID
            <L[2]              
               <U2 [1]>         = EquipStatusID 
               <U2 [1]>         = PPExecNameID     
              
            >
        >        
    >
>.
S2F33 OUTPUT = s2f33outmulti W
<L [2]
    <U2 [1]>=DataID
    <L[1]
        <L [2]
            <U2 [1]>              =ReportID
            <L[5]
               <U2 [1]>          = VariableID0   
               <U2 [1]>          = VariableID1
               <U2 [1]>          = VariableID2
               <U2 [1]>          = VariableID3
               <U2 [1]>          = VariableID4
            >
        >        
    >
>.
****************************************************************************
* S2F34 Define Report Acknowledge (DRA)                        H <- E      *
****************************************************************************

S2F34 INPUT = s2f34in 

<B[1]>=AckCode.


****************************************************************************
* S2F35    Link Event Report (LER)                             H -> E      *
****************************************************************************
S2F35 OUTPUT = s2f35out W
<L[2]
    <U2 [1]> = DataID
     <L[1]
        <L [2]
            <U2 [1]> = CollEventID
            <L[1]
                <U2 [1]> = ReportID
            >
        >
    >
>.

****************************************************************************
* S2F36   Link Event Report Acknowledge(LERA)                  E -> H      *
****************************************************************************

S2F36 INPUT = s2f36in 
<B [1]> = AckCode.


****************************************************************************
* S2F37   Enable / Disable Event Report (EDER)                 H -> E      *
****************************************************************************

S2F37 OUTPUT = s2f37out W
<L[2]
    <BOOLEAN [1]> = Booleanflag
    <L[1]
        <U2 [1]> = CollEventId
    >
>.
****************************************************************************
* S2F38  Enable / Disable Event Report Acknowledge (EERA)      E -> H      *
****************************************************************************

S2F38 INPUT = s2f38in 
<B [1]> = AckCode.
****************************************************************************
* S2F45 Define Variable Limit Attributes(DVLA)                 E <- H      *
****************************************************************************

S2F45 = s2f45singleout OUTPUT W 
<L[2]
 <U2 [1] > = DataId
  <L [2] 
    <U2 [1]> = VID
      <L[2]
        <B [1]> = LimitId
            <L[2]
                <F4 [1]> =Upper
                <F4 [1]> =Lower
        >
      >
  >
>.
S2F45 = s2f45out OUTPUT W 
<L[2]
 <U2 [1] > = DataId
 <V> = Limit
>.
****************************************************************************
* S2F47 Variable Limit Attribute  Request                    E -> H      *
****************************************************************************

S2F47 = s2f47out OUTPUT W 
<L[0]>.
****************************************************************************
* S2F48 Variable Limit Attribute  Request                    E -> H      *
****************************************************************************

S2F48 = s2f48out OUTPUT  
<L[0]>.
****************************************************************************
* S5F1 Alarm Report Send                    E -> H      *
****************************************************************************
S5F1 = s5f1in INPUT W
<L[3]
    <B [1] > = ALCD
    <U2 [1] > = ALID
    <A [MAX 100]> = ALTX
>.
S5F1 = s5f1ypmin INPUT W
<L[3]
    <B [1] > = ALCD
    <U4 [1] > = ALID
    <A [MAX 100]> = ALTX
>.
****************************************************************************
* S5F2 Alarm Report ACK                    E -> H      *
****************************************************************************
S5F2 = s5f2out OUTPUT 
    <B [1]> = AckCode.
  
****************************************************************************
* S5F3  Enable Alarm Send                    E <- H      *
****************************************************************************
S5F3 OUTPUT = s5f3allout  W 
<L [2]
    < B [1] > = ALED
    < L [0]>
>.
S5F3 = s5f3out OUTPUT W 
<L[2]
    <B [1]> = ALED
    <U4 [1]> = ALID   
>.
****************************************************************************
* S5F4 Enable Alarm Report ACK                    E -> H         *
****************************************************************************
S5F4 = s5f4in INPUT 
    <B [1]> = AckCode.  
****************************************************************************
* S5F5 List Alarm Request                    E <- H         *
****************************************************************************
S5F5 = s5f5out OUTPUT W
    <L [0]> .  
****************************************************************************
* S5F7 List Enableed Alarm Request                    E <- H         *
****************************************************************************
S5F7 = s5f7out OUTPUT W
    <L [0]> .  
****************************************************************************
* S5F8 List Enableed Alarm Respones                    E <- H         *
****************************************************************************
S5F8 = s5f8in INPUT 
    <V> = RESULT.  
****************************************************************************
* S7F1    Process Program Load Inquire                          E<->H      *
****************************************************************************

S7F1  = s7f1out OUTPUT W
<L[2]
    <A [MAX 100] >             = ProcessprogramID
    <U4 [1]>                   = Length
>.

S7F1  = s7f1in INPUT W
<L[2]
    <A [MAX 100] >               = ProcessprogramID
    <U4 [1]>                    = Length
>.

****************************************************************************
* S7F2   Process Program Load Grant                          E<->H      *
****************************************************************************
S7F2 OUTPUT = s7f2out
<B [1]> = PPGNT.

S7F2 INPUT = s7f2in
<B [1]> = PPGNT.

****************************************************************************
* S7F3   Process Program Send                                E<->H      *
****************************************************************************
S7F3  = s7f3out OUTPUT W
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <V>              =Processprogram
>.
S7F3 INPUT =s7f3in W
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <A [MAX 100] >                   =Processprogram
>.

****************************************************************************
* S7F4   Process Program Acknowledge                          E<->H      *
****************************************************************************
S7F4 OUTPUT = s7f4out
<B [1]> = AckCode.

S7F4 INPUT =s7f4in
<B [1]> = AckCode.

****************************************************************************
* S7F5  Process Program Request                                 E<->H      *
****************************************************************************
S7F5 OUTPUT = s7f5out W
<A [MAX 100] >= ProcessprogramID.

S7F5 INPUT = s7f5in W
<A [MAX 100] >= ProcessprogramID.

****************************************************************************
* S7F6  Process Program Data                                    H<->E      *
****************************************************************************

S7F6 OUTPUT = s7f6out
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <V>                     = Processprogram
>.
S7F6 = s7f6in INPUT 
<L[2]
    <A [MAX 100] >               =  ProcessprogramID 
    <V>                     = Processprogram
>.
S7F6 = s7f6zeroin INPUT 
<L[0]   
>.
****************************************************************************
* S7F9 M/P  M Request(MMR)                                     E<-> H      *
****************************************************************************
S7F9 INPUT = s7f9in  W.

S7F9 OUTPUT = s7f9out  W.

****************************************************************************
* S7F10 M/P  M Data(MMR)                                       E<-> H      *
****************************************************************************
S7F10 = s7f10in INPUT 
<L[1]
    <L[2]
    <A [MAX 100]> = ProcessprogramID
    <L[1]
    <A [MAX 100]> = MaterialID
    >
    >
>.
****************************************************************************
* S7F17 Delete Process Program Send                              H->E      *
****************************************************************************
S7F17  = s7f17out OUTPUT W
<L [1]
    <A [MAX 100]>                = ProcessprogramID 
>.

****************************************************************************
* S7F18 Delete Process Program Acknowledge                       E->H      *
****************************************************************************
S7F18 INPUT = s7f18in
<B [1]> = AckCode.

****************************************************************************
* S7F19 Current EPPD Request                                     H->E      *
****************************************************************************
S7F19  = s7f19out OUTPUT W.

S7F19 = s7f19in INPUT W.
****************************************************************************
* S7F20 Current EPPD Data                                        E->H      *
****************************************************************************
S7F20 INPUT = s7f20in

    <V>                = EPPD.

****************************************************************************
* S7F21 Equipment Process Cabpabiloties Request                  E<-H      *
***************************************************************************
S7F21 = s7f21in INPUT W.
S7F21 = s7f21out OUTPUT W.
****************************************************************************
* S7F22 Equipment Process Cabpabiloties Data                  E->H      *
***************************************************************************
S7F22 =s7f22in INPUT
<L[5]
    <A [ MAX 6] > = Mdln
    <A [MAX 6] > = SoftRev
    <U1 [1]> = CommandMax
    <U4 [1]> = BtyeMax
    <L[1]
        <L[11]
           <U2 [1]> =  Commandcode
            <A [MAX 16]> = Commandname
            <BOOLEAN [1]> = RequiedCommand
            <I1 [1]> = Blockdefinition 
            <I2 [1]> = BeforeCommandCodes
            <U2 [1]> = ImmediatelyBeforeCommandCodes
            <U2 [1]> = NotBeforeCommandCodes
            <U2 [1]> = AfterCommandCodes
            <U2 [1]> = ImmediatelyAfterCommandCodes
            <U2 [1]> = NotAfterCommandCodes
            <L [9]
                <A [MAX 16]> = ParameterName
                <BOOLEAN [1]> = RequiedParameter
                <F8 [1] > = ParameterDefaultValue
                <U2 [1] > = ParameterCountMaximum
		<F8 [1] > =LowerLimitForNumericValue
		<F8 [1] > =UpperLimitForNumericValue
		<A [MAX 10] > =UnitsIdentifier
		<I1 [1] > =ResolutionCodeForNumericData
		<F8 [1] > =ResolutionValueForNumericData
            >
        >
    >
>.
 ****************************************************************************
* S7F23 Formatted Process Program Send                        E<-> H     *
****************************************************************************
S7F23 =s7f26out OUTPUT
<L [1]
 <V > = RESULT	
>.

 ****************************************************************************
* S7F25 Formatted Process Program Requet                        E<-> H     *
****************************************************************************
S7F25  = s7f25in INPUT  
<A [MAX 100]> = ProcessprogramID.
S7F25 OUTPUT = s7f25out   W
<A [MAX 100]> = ProcessprogramID.


****************************************************************************
* S7F26 Formatted Process Program Request                     E< -> H      *
****************************************************************************
S7F26 = s7f26onein INPUT
<L[4]
    <A [MAX 100] > = ProcessprogramID
    <A [MAX 6] > = Mdln
    <A [MAX 6] > = SoftRev
    <L[1]
        <L[2]
     <A [MAX 90] >  =Commandcode
        <L[1]
      <V>      = ProcessParameter
        >
    >
>
>.
S7F26 =s7f26in INPUT
<V>=RESULT.

****************************************************************************
* S7F27  Process Program Verification Send                    E -> H      *
****************************************************************************
S7F27 = s7f27in INPUT W
<L[2]
<A [MAX 100] > = ProcessprogramID
    <L[1]
        <L[3]
        <B [1]> =AckCode
        <U1 [1]> = CommandNumber
        <V> = ERRW7
        >
    >
>.
****************************************************************************
* S7F28  Process Program Verification Acknowledge              E< - H      *
****************************************************************************
S7F28 = s7f28out OUTPUT. 
****************************************************************************
* S7F33  Process Program Available Request             E<-> H      *
****************************************************************************
S7F33 = s7f33out OUTPUT W
<L[1]
<A [MAX 100] > = ProcessprogramID
>.
****************************************************************************
* S6F15  Event Report REQUEST (ERS)                               E< -H     *
****************************************************************************
 S6F15 =s6f15out  OUTPUT W
<L [0]	
	
>. 
**************************************** ************************************
*S2F41 Host Command Send(HCS)                                         H->E*
****************************************************************************
S2F41  = s2f41out OUTPUT W
<L[2]
    <A[MAX 80]> = Remotecommand
    <L[1]
        <L[2]
        <A[MAX 80]> = Commandparametername
        <A[MAX 80]> = Commandparametervalue
        >
     
    >
>.
S2F41  = s2f41zeroout OUTPUT W
<L[2]
    <A[MAX 80]> = Remotecommand
    <L [0]       
    >
>.
S2F41  = s2f41testout OUTPUT W
<L[1]
    <A[MAX 80]> = Remotecommand  
>.
S2F41  = s2f41perout OUTPUT W
<L[2]
    <A[MAX 80]> = Remotecommand
    <L [1]
        <L[2]
            <L[2]
                <A [MAX 100]> =INTERVALSTART
                <A [MAX 100]> =TIMESTART
                >
            <L[2]
                <A [MAX 100]> =INTERVALEND
                <A [MAX 100]> =TIMEEND
                >
        >
    >
>.
S2F41  = s2f41outPPSelect OUTPUT W
<L[2]
    <A 'PP-SELECT'> 
    <L[1]
        <L[2]
            <A 'PPID'> 
            <A [MAX 100]> = PPID
        >    
    >
>.
S2F41  = s2f41outConfig OUTPUT W
<L[2]
    <A [MAX 50]> = RCMD
    <V> = CPLIST
>.
**************************************** ************************************
*S2F42 Host Command Send(HCS)                                         H<-E*
****************************************************************************
S2F42 = s2f42in INPUT 
<L [2]
    <B [1] > = HCACK 
    <L[2]
        <A [MAX 50]> = PARAMETERNAME
        <B [1] > = CPACK
    >
>.
S2F42 = s2f42zeroin INPUT 
<L [2]
    <B [1] > = HCACK 
    <L[0]
    >
>.