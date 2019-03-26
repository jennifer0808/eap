****************************************************************************
*  Filename    : SigmaPlusHOST.SML
*  Description : Pfile for the HOST side of the SigmaPlus8800
*  Author      : hecongyuan
*  Date        : 03/25/2016
*  Modify      : luosy
*  Modify date : 02/23/2017
****************************************************************************
* Find out MAX_SVIDREQ, MAX_IMAGES & MAX_COMPARM values
#define MAX_STRIP_MAP_DATA_LENGTH   200000
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

S1F2 =s1f2outListZero OUTPUT
  <L [0]>.
S1F2 =s1f2out OUTPUT
  <L [0]>.
S1F2 =s1f2in INPUT
  <L [2]
      <A [MAX 6] >               = Mdln
      <A [MAX 6] >               = SoftRev
  >.
****************************************************************************
* S1F3  Selected Equipment Status Request (SSR)                H  -> E     *
****************************************************************************

S1F3 = s1f3statecheck OUTPUT W
 <L [3]
    <U4 [1]> = EquipStatus
    <U4 [1]> = PPExecName
    <U4 [1]> = ControlState
>.
S1F3 = s1f3Specific OUTPUT W
<V>=DATA.
S1F3 =s1f3singleout OUTPUT W
<L [1]
    <U4 [1]> = SVID
>.
S2F13 = s2f13Specific OUTPUT W
       <V>=DATA.
S2F13 =s2f13singleout OUTPUT W
<L [1]
    <U4 [1]> = ECID
>.
S2F15 =s2f15out OUTPUT W
< L [1]
    < L [2]
        <U2 [1]> = ECID
        <A [MAX 30000]>= ECV        
    >
>.
S2F15 =s2f15out111 OUTPUT W
< L [4]
    < L [2]
        <U2 [1]> = ECID
        <A [MAX 3000]>= ECV        
    >
    < L [2]
        <U2 [1]> = ECID
        <A [MAX 3000]>= ECV        
    >
    < L [2]
        <U2 [1]> = ECID
        <A [MAX 3000]>= ECV        
    >
    < L [2]
        <U2 [1]> = ECID
        <A [MAX 3000]>= ECV        
    >
>.
S2F15 =s2f15out1 OUTPUT W
< L [1]
    < L [2]
        <U2 [1]> = ECID
        <L [2]
            <A [MAX 50]> = GROUPID
            <A [MAX 50]> = PARAMETERID
        >
    >
>.
S2F15 = s2f15out3 OUTPUT  W
  <L [2]
     <L[2]
         <U2 [1]> = EC096
         <A [MAX 60] > = GROUPID
     >
     <L[2]
         <U2 [1]> = EC097
         <A [MAX 50]> = PARAMETERID
     >
  >.
****************************************************************************
* S2F16   New Equipment Constant ACK                         H<- E      *
****************************************************************************
S2F16 =s2f16in1 INPUT 
< B [1]> = AckCode.
S2F16 =s2f16in INPUT 
<V> = AckCode.
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

S2F30 =s2f30onein INPUT 
    <V> = RESULT.

****************************************************************************
* S1F4    Selected Equipment Status                       E -> H     *
****************************************************************************
S1F4 =s1f4in INPUT 
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

S1F13 = s1f13outListZero OUTPUT W
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
* S6F11  Event Report Send (ERS)                               E -> H      *
****************************************************************************
S6F11 W =s6f11EquipStatusChange INPUT
<L [3]
	 <U4 [1] >                    = DataId
         <U4 [1] >                    = CollEventID	
	<L [1]
		<L [2]
			<U4[1] >=rptid
			<L [2]                   
                                <U1[1] >=EquipStatus
				<A[MAX 100] >=PPExecName
				*<U1[1] >=ControlState	
			>
		>
	>
>.  

  
S6F11 = s6f11inCommon1  INPUT W
 <L [3]
         <U4 [1] >                    = DataId
         <U4 [1] >                    = CollEventId
         <L [0]
         >
  >.
S6F11 W =s6f11inCommon2 INPUT
<L [3]
	 <U4 [1] >                    = DataId
         <U4 [1] >                    = CollEventId	
	<L [1]
		<L [2]
			<U4[1] >=rptid
			<L [2]
				<U1[1] >=EquipStatus
				<A[MAX 100]>=PPExecName
			>
		>
	>
>. 
S6F11 W =s6f11inCommon3 INPUT
<L [3]
	 <U4 [1] >                    = DataId
         <U4 [1] >                    = CollEventId	
	<L [1]
		<L [2]
			<U4[1] >=rptid
			<L [2]
                          <A [MAX 66] >  = Clock			
                          <U1[1] >=PreviousProcessState			
			>
		>
	>
>.  

  S6F11 = s6f11inStripMapUpload  INPUT W
 <L [3]
         <U4 [1] >                    = DataId
         <U4 [1] >                    = CollEventId
         <L [1]
           <L [2]
             <U4 [1] >                = ReportId
             <L [1]
               <V>                    = MapData
             >                    
           >
         >
  >.
 S6F11 W =s6f11inCommon4 INPUT
<L [3]
	<U4[1] > = DataId
	<U4[1] > = CollEventId
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [2]
				<U2[1] > = ECID
				<A[MAX 200]> = ECV
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
S10F1  = s10f1in  INPUT W
<L [2]
        <V>      = TID
        <A [MAX 1000]>   = TEXT
>.
S10F2 output = s10f2out
 <B [1]>        = AckCode
.
 
S10F3 output = s10f3out W
<L [2]
        <B[1]>      = TID
        <A [MAX 1000]>   = TEXT
>.

S10F4 INPUT = s10f4in
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

S14F2 OUTPUT = s14f2outNoExist
<L [2]
    <L [0]>
    <L [2]
      <U1 [1]>                    = ObjectAck
      <L [0]>
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

S2F13 =s2f13ESECDB2100FCECRecipePara OUTPUT W
< L [24]
 <U4 [1]>=Data0
<U4 [1]>=Data1
<U4 [1]>=Data2
<U4 [1]>=Data3
<U4 [1]>=Data4
<U4 [1]>=Data5
<U4 [1]>=Data6
<U4 [1]>=Data7
<U4 [1]>=Data8
<U4 [1]>=Data9
<U4 [1]>=Data10
<U4 [1]>=Data11
<U4 [1]>=Data12
<U4 [1]>=Data13
<U4 [1]>=Data14
<U4 [1]>=Data15
<U4 [1]>=Data16
<U4 [1]>=Data17
<U4 [1]>=Data18
<U4 [1]>=Data19
<U4 [1]>=Data20
<U4 [1]>=Data21
<U4 [1]>=Data22
<U4 [1]>=Data23
>.

****************************************************************************
* S2F14    Equipment Constant Data                          H<- E      *
****************************************************************************

S2F14 =s2f14in INPUT 
< V >=RESULT.

****************************************************************************
* S2F33    Define Report (DR)                                  H -> E      *
****************************************************************************
S2F33 OUTPUT = s2f33clear W
  <L[2]
         <U4[1] 0>
         <L[0]>
     >.

S2F35 OUTPUT = s2f35clear W
  <L[2]
         <U4[1] 0>
         <L[0]>
     >.
S2F33 OUTPUT = s2f33out W
<L [2]
    <U4 [1]>                     = DataID
    <L[1]
        <L [2]
            <U4 [1]>             = ReportID
            <L[1]
               <U2 [1]>          = VariableID            
            >
        >        
    >
>.
S2F33 OUTPUT = s2f33eqpstate W
<L [2]
    <U4 [1]>=DataID
    <L[1]
        <L [2]
            <U4 [1]>            =ReportID
            <L[2]              
               <U2[1]>         = EquipStatusID 
               <U2[1]>         = PPExecNameID                  
            >
        >        
    >
>.
S2F33 OUTPUT = s2f33state W
<L [2]
    <U4 [1]>=DataID
    <L[1]
        <L [2]
            <U4[1]>            =ReportID
            <L[3]              
               <U4 [1]>         = EquipStatusID 
               <U4 [1]>         = PPExecNameID    
               <U4 [1]>         = ControlStateID                  
            >
        >        
    >
>.
S2F33 OUTPUT = s2f33zeroout W
<L [2]
    <U4 [1]>=DataID
    <L[1]
        <L [2]
            <U4 [1]>        =ReportID
            <L[0]                       
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
    <U4 [1]> = DataID
     <L[1]
        <L [2]
            <U4 [1]> = CollEventID
            <L[1]
                <U4 [1]> = ReportID
            >
        >
    >
>.
S2F35 OUTPUT = s2f35zeroout W
<L[2]
    <U4 [1]> = DataID
     <L[1]
        <L [2]
            <U4 [1]> = CollEventID
            <L[0]                
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
        <U4 [1]> = CollEventId
    >
>.

S2F37 OUTPUT = s2f37outAll W
<L[2]
    <BOOLEAN [1]> = Booleanflag
    <L[0]
    >
>.
****************************************************************************
* S2F38  Enable / Disable Event Report Acknowledge (EERA)      E -> H      *
****************************************************************************

S2F38 INPUT = s2f38in 
<B [1]> = AckCode.
S2F41  = s2f41outPPSelect OUTPUT W
<L[2]
    <A 'PP_SELECT'> 
    <L[1]
        <L[2]
            <A 'PROGRAM'> 
            <A [MAX 100]> = PPID
        >    
    >
>.
S2F41  = s2f41zeroout OUTPUT W
<L[2]
    <A[MAX 80]> = Remotecommand
    <L [0]       
    >
>.
S2F42 INPUT = s2f42in
<L [2]
	<B[1] >=HCACK
	<L [0]
	>
>. 
S2F41  = s2f41stopout OUTPUT W
<L[2]
    <A'STOP'>
      <L[1]
        <L[2]
            <A 'RCText'> 
            <A 'CIMSTOP'>
        >    
    >
>.
****************************************************************************
* S5F1   Alarm Report Send (ARS)                               H <- E      *
****************************************************************************

S5F1 = s5f1in INPUT W
<L[3]
    <B [1] > = ALCD
    <V> = ALID
    <A [MAX 200]> = ALTX
>.

****************************************************************************
* S5F2   Alarm Report Acknowledge (ARA)                        H -> E      *
****************************************************************************

S5F2 = s5f2out      OUTPUT
  <B [1] >                               = AckCode.
****************************************************************************
* S5F3  Enable Alarm Send                    E <- H      *
****************************************************************************
S5F3 OUTPUT = s5f3allout  W 
<L [2]
    < B [1] > = ALED 
    <U4 [0]>
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
S7F1  = s7f1multiout OUTPUT W
    <V> = VALUE.
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
    <V>                     =Processprogram
>.
S7F3 INPUT =s7f3in W
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <V>                   =Processprogram
>.
S7F3  = s7f3multiout OUTPUT W
   <V> = VALUE.
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
S7F6 = s7f6in INPUT 
<L[2]
    <A [MAX 100] >               =  ProcessprogramID 
    <V>                     = Processprogram
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
    <V>   = EPPD.
****************************************************************************
* S7F25  Process Program Request                                 E<->H      *
****************************************************************************
S7F25 OUTPUT = s7f25out W
<A [MAX 100] >= ProcessprogramID.

S7F25 INPUT = s7f25in W
<A [MAX 100] >= ProcessprogramID.

****************************************************************************
* S7F26  Process Program Data                                    H<->E      *
****************************************************************************
S7F26 = s7f26in INPUT 
<L[1]
    <V>  = RESULT
>.
****************************************************************************
* S12F1  Map setup  Data Send(MSDS)                            H <- E   w  *
****************************************************************************
S12F1 INPUT = s12f1in W
<L[15]
    <V>=MaterialID
    <B [1]>=IDTYP
    <U2 [1]>=FlatNotchLocation
    <U2 [1]>=FileFrameRotation
    <B [1]>=OriginLocation
    <U1 [1]>=RrferencePointSelect
    <V>=REFPxREFPy
    <A[MAX 100]>=DieUnitsOfMeasure
    <V>=XAxisDieSize
    <V>=YAxisDieSize
    <U2[1]>=RowCountInDieIncrements
    <U2[1]>=ColumnCountInDieIncrements
    <V>=NullBinCodeValue
    <U2[1]>=ProcessDieCount
    <B [1]>=ProcessAxis
>.
****************************************************************************
* S12F2  Map setup  Data ACK(MSDA)                            H -> E      *
****************************************************************************
S12F2 OUTPUT = s12f2out 
<B [1]>=SDACK.
****************************************************************************
* S12F3  Map setup  Data Request(MSDR)                         H <- E   w  *
****************************************************************************
S12F3 INPUT = s12f3in W
<V> = RESULT.
*S12F3 INPUT = s12f3in1 W
*<L[9]
    *<V>=MaterialID
   * <B [1]>=IDTYP
   * <B [1]>=MapDataFormatType
   * <U2 [1]>=FlatNotchLocation
   * <U2 [1]>=FileFrameRotation
    *<B [1]>=OriginLocation
   * <B [1]>=ProcessAxis
    *<V>=BinCodeEquivalents
   * <V>=NullBinCodeValue
*>.
****************************************************************************
* S12F4  Map setup  Data (MSD)                            H -> E     *
****************************************************************************
S12F4 OUTPUT = s12f4out
<L[15]
    <A[MAX 100]>=MaterialID
    <B [1]>=IDTYP
    <U2 [1]>=FlatNotchLocation
    <B [1]>=OriginLocation
    <U1 [1]>=RrferencePointSelect
    <L[0]>
    <A[MAX 100]>=DieUnitsOfMeasure
    <U4[1]>=XAxisDieSize
    <U4[1]>=YAxisDieSize
    <U2[1]>=RowCountInDieIncrements
    <U2[1]>=ColumnCountInDieIncrements
    <U4[1]>=ProcessDieCount
    <U1[0]>
    <U1[1]>=NullBinCodeValue  
    <U2 [1]>=MessageLength
>.

****************************************************************************
* S12F5  Map Transmit  Inquire (MAPTI)                            H <- E   *
****************************************************************************
S12F5 INPUT = s12f5in W
<L[4]
    <V>=MaterialID
    <B [1]>=IDTYP
    <B [1]>=MapDataFormatType
    <U4 [1]>=MessageLength
>.
****************************************************************************
* S12F6  Map Transmit  Grant (MAPTG)                            H -> E      *
****************************************************************************
S12F6 OUTPUT = s12f6out 
<B [1]>=GRANT1.
****************************************************************************
* S12F7  Map Data Send Type 1 (MDS1)                            H <- E   *
****************************************************************************
S12F7 INPUT = s12f7in W
<L[3]
    <V>=MaterialID
    <B [1]>=IDTYP
    <V>=RSINFBinList
>.
****************************************************************************
* S12F8  Map Data Acknowledge Type1 (MDA1)                        H <- E   *
****************************************************************************
S12F8 OUTPUT = s12f8out 
<B [1]>=MDACK.
****************************************************************************
* S12F9  Map Data Send Type 2 (MDS2)                            H <- E   *
****************************************************************************
S12F9 INPUT = s12f9in W
<L[4]
    <V>=MaterialID
    <B [1]>=IDTYP
    <V>=STRPxSTRPy
    <V>=BinList
>.
****************************************************************************
* S12F10  Map Data Acknowledge Type2 (MDA2)                       H <- E   *
****************************************************************************
S12F10 OUTPUT = s12f10out 
<B [1]>=MDACK.
****************************************************************************
* S12F11  Map Data Send Type 3 (MDS3)                            H <- E   *
****************************************************************************
S12F11 INPUT = s12f11in W
<L[3]
    <V>=MaterialID
    <B [1]>=IDTYP
    <V>=XYPOSBinList
>.
****************************************************************************
* S12F12  Map Data Acknowledge Type3 (MDA3)                            H <- E   *
****************************************************************************
S12F12 OUTPUT = s12f12out 
<B [1]>=MDACK.
****************************************************************************
* S12F13  Map Data Request Type 1 (MDR1)                          H <- E   *
****************************************************************************
S12F13 INPUT = s12f13in W
<L[2]
    <V>=MaterialID
    <B [1]>=IDTYP
>.
****************************************************************************
* S12F14  Map Data  Type1 (MD1)                            H <- E   *
****************************************************************************
S12F14 OUTPUT = s12f14out 
<L[3]
    <V>=MaterialID
    <B [1]>=IDTYP
    <V>=RSINFBinList
>.
****************************************************************************
* S12F15  Map Data Request Type 2 (MDR2)                          H <- E   *
****************************************************************************
S12F15 INPUT = s12f15in W
<L[2]
    <A[MAX 100]>=MaterialID
    <B [1]>=IDTYP
>.
****************************************************************************
* S12F16  Map Data  Type2 (MD2)                            H <- E   *
****************************************************************************
S12F16 OUTPUT = s12f16out 
<L[4]
    <A[MAX 100]>=MaterialID
    <B [1]>=IDTYP
    <I2[2]>=STRPxSTRPy
    <V>=BinList
>.
****************************************************************************
* S12F17  Map Data Request Type 3 (MDR3)                          H <- E   *
****************************************************************************
S12F17 INPUT = s12f17in W
<L[3]
    <V>=MaterialID
    <B [1]>=IDTYP
    <V>=SendBinInformationFlag
>.
****************************************************************************
* S12F18  Map Data  Type3 (MD3)                            H <- E   *
****************************************************************************
S12F18 OUTPUT = s12f18out 
<L[3]
    <V>=MaterialID
    <B [1]>=IDTYP
    <V>=XYPOSBinList
>.
****************************************************************************
* S12F19  Map Error Report Send (MERS)                           H <-> E   *
****************************************************************************
S12F19 OUTPUT = s12f19out 
<L[2]
    <V>=MapError
    <V>=DataLoaction
>.
