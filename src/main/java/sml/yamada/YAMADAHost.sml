****************************************************************************
*  Filename    : YAMADAHOST.SML
*  Description : Pfile for the HOST side of the YAMADA
*  Author      : luosy
*  Date        : 05/26/2016
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
S2F0 =s2f0out OUTPUT.

S2F0 =s2f0in INPUT.
S7F0 =s7f0out OUTPUT.

S7F0 =s7f0in INPUT.
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
    <U4 [1]> = SVID
>.

S1F3 =s1f3statecheck OUTPUT W
 <L [3]
    <U4 [1]> = EquipStatus
    <U4 [1]> = PPExecName
    <U4 [1]> = ControlState
>.

S1F3 = s1f3rcpandstate OUTPUT W
 <L [3]
    <U4 [1]>=EquipStatus
    <U4 [1]>=PreProcessStatus
    <U4 [1]>=PPExecName
>.

S1F3 = s1f3pressout OUTPUT W
<L [4]
    <U4 [1]> = Press1
    <U4 [1]> = Press2
    <U4 [1]> = Press3
    <U4 [1]> = Press4
>.

S1F3 =s1f3YAMADA170TRcpPara OUTPUT W
<L[4]
<U4 [1]>=Data0
<U4 [1]>=Data1
<U4 [1]>=Data2
<U4 [1]>=Data3
>.

S1F3 = s1f3Specific OUTPUT W
<V>=DATA.

****************************************************************************
* S1F4    Selected Equipment Status                       E -> H     *
****************************************************************************
 S1F4 =s1f4in INPUT 
  <V> = RESULT.

****************************************************************************
* S1F11    Status Values Namelists Request  (svnr)              E <- H     *
****************************************************************************
S1F11 = s1f11out OUTPUT W
  <L [0]>.

****************************************************************************
* S1F12    Status Values Namelists Reply  (svnrr)                   E ->H  *
****************************************************************************
S1F12 = s1f12in INPUT 

    <A [MAX 5000]> = RESULT.
  

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
* S2F17  Date and Time Request                 H -> E     *
****************************************************************************
S2F17 = s2f17in INPUT W.
****************************************************************************
* S2F18  Date and Time Data                H <- E     *
****************************************************************************
S2F18  = s2f18out OUTPUT
<A [MAX 100]>=Time.
****************************************************************************
* S6F11  Event Report Send (ERS)                               E -> H      *
****************************************************************************
S6F11 = s6f11incommon111  INPUT W
<L [3]
	<U4[1] >   = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [2]
				<A[16] >=date
				<B[1] >=ControlState
			>
		>
	>
>. 

S6F11 = s6f11incommon  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [3]
             <F4 [1]>                     =Data1
             <U4 [1]>                     =Data2
             <U4 [1]>                     =Data3
           >                    
         >
       >
 >.

S6F11 = s6f11incommon1  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [2]
             <A [MAX 60]>                     =Data1
             <I2 [1] >                        =Data2
           >                    
         >
       >
 >.

S6F11 = s6f11incommon2  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [3]
             <A [MAX 60]>                     =Data1
             <I2 [1] >                        =Data2
             <I2 [1] >                        =Data3
           >                    
         >
       >
 >.

S6F11 = s6f11incommon3  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [2]
             <A [MAX 60]>                     =Data1
             <U1 [1] >                        =Data2
           >                    
         >
       >
 >.

S6F11 = s6f11incommon4  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [2]
             <I2 [1] >                     =Data2
             <I2 [1] >                     =Data3
           >                    
         >
       >
 >.

S6F11 = s6f11incommon5  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [1]
             <I2 [1] >                     =Data1
           >                    
         >
       >
 >.

S6F11 = s6f11incommon6 INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [3]
             <A [MAX 60]>                     =Data1
             <A [MAX 60]>                     =Data2
             <A [MAX 60]>                     =Data3
           >                    
         >
       >
 >.

S6F11 = s6f11incommon7  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [4]
             <U4 [1]>                         =Data2
	     <U1 [1]>                         =Data3
	     <A [MAX 60]>                     =Data1
	     <L [0]
	     >
           >                    
         >
       >
 >.

S6F11 = s6f11incommon8  INPUT W
<L [3]
	<U4 [1]> = DataID
	<U4 [1]>  = CollEventID
	<L [2]
		<L [2]
			<U4 [1]>   = Data5
			<L [2]
				<A [MAX 60]>                     =Data1
				<A [MAX 60]>                     =Data2
			>
		>
		<L [2]
			<U4 [1]> = Data6
			<L [2]
				<A [MAX 60]>                     =Data3
				<A [MAX 60]>                     =Data4
			>
		>
	>
>. 
S6F11 = s6f11workloaded INPUT W 
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > =ReportId
			<L [4]
				<A[MAX 60] > = Data1
				<A[MAX 60] > = Data2
				<A[MAX 60]> = Data3
				<U4 [1] > = Data4
			>
		>
	>
>. 

  S6F11 = s6f11incommon11  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [3]
             <A [MAX 60]>                     =Data1
            <U4 [1] >                        = Data2
             <A [MAX 60]>                     =Data3         
           >                    
         >
       >
 >.

S6F11 = s6f11incommon12  INPUT W
<L [3]
	<U4[1] >    = DataID
	<U4[1] >     = CollEventID
	<L [2]
		<L [2]
			<U4[1] >  =DATA1
			<L [1]
				<A[MAX 100] > = TimeStr
			>
		>
		<L [2]
			<U4[1] >   =DATA2
			<L [2]
				<L [3]
					<U4[1] >  =DATA3
					<U4[1] >  =DATA4
					<U4[1] >  =DATA5
				>
				<U4[1] > =DATA6
			>
		>
	>
>. 

S6F11 = s6f11incommon13  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [4]
             <A  [MAX 50]>                    = TimeStr
             <I2 [1] >                        = Data10
             <I2 [1] >                        = Data11
	     <A [0] >
           >                    
         >
       >
 >.
S6F11 = s6f11incommon14  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [1]
               <L[3]
                 <A  [MAX 50]>                = DATA1
                 <U1 [1]>                     = DATA2
                 <L[1]                       
                    <L [3] 
			<A[MAX 50]>           = DATA3
			<A[MAX 50]>           = DATA4
			<A[MAX 50]>           = DATA5
                    >
                 >
               >                    
             >
           >
       >
 >.
S6F11 = s6f11incommon15  INPUT W
<L [3]
	<U4 [1] >                             = DataID
	<U4 [1]>                              = CollEventID
	<L [1]
		<L [2]
			<U4 [1]>              = ReportId
			<L [2]
                            <A [MAX 50]>      = DATA1
			    <I4 [1]>          = DATA2
			>
		>
	>
>. 
S6F11 = s6f11incommon16  INPUT W
<L [3]
	<U4[1] >=DataID
	<U4[1] >=CollEventID
	<L [1]
		<L [2]
			<U4[1] >= ReportId
			<L [2]
				<A[MAX 100] >=Time
				<B[1] >=ControlState
			>
		>
	>
>.

S6F11 = s6f11equipstate  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [2]
             <A [MAX 60]>                     =Data1
             <A [MAX 60]>                     =Data2         
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
S6F11 =s6f11equipstatuschange INPUT W 
<L [3]
	<U4[1] > = DataID
	<U4[1] >= CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [3]
				<A[16] >=date
				<U1[1] >=EquipStatus
				<U1[1] >=PreEquipStatus
			>
		>
	>
>. 
S6F11 =s6f11RunModeChange INPUT W 
<L [3]
	<U4[1] >=DataID
	<U4[1] >= CollEventID
	<L [1]
		<L [2]
			<U4[1] >= ReportId
			<L [2]
				<A[16] >=date
				<U2[1] >=RunMode
			>
		>
	>
>.

S6F11 =s6f11incommon119 INPUT W 
<L [3]
	<U4[1] >= DataID
	<U4[1] >= CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [1]
				<A[16] >=date
			>
		>
>
>.
S6F11 = s6f11incommon118 INPUT W 
<L [3]
	<U4[1]>                               = DataID
	<U4[1]>                               = CollEventID
	<L [1]
		<L [2]
			<U4[1]>               = ReportId
			<L [4]
				<A[MAX 50]>   = TimeStr
				<A[MAX 50]>   = OpName
				<A[MAX 50]>   = RecipeName
				<A[MAX 50]>   = Remark
			>
		>
	>
>. 
 
S6F11 =s6f11lotstartready  INPUT W
<L [3]
	<U4[1] >= DataID
	<U4[1] >= CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [5]
				<A[16]>=date
				<A[MAX 100]>=PPExecName
				<A[MAX 100]>=datA1
				<A[MAX 100]>=datA2
				<A[MAX 100]>=datA3
			>
		>
	>
>. 
S6F11 =s6f11incommon17 INPUT W 
<L [3]
	<U4[1] >=  DataID
	<U4[1] >=  CollEventID
	<L [1]
		<L [2]
			<U4[1] > =ReportId
			<L [39]
				<A[MAX 50] > = TimeStr
				<A[MAX 50] > = PPExecName
				<A[MAX 50] > = Remark
				<U2[1] > =Data0
				<U4[1] > =Data1
				<F4[1] > =Data2
				<F4[1] > =Data3
				<F4[1] > =Data4
				<F4[1] > =Data5
				<F4[1] > =Data6
				<F4[1] > =Data7
				<F4[1] > =Data8
				<F4[1] > =Data9
				<F4[1] > =Data10
				<F4[1] > =Data11
				<F4[1] > =Data12
				<F4[1] > =Data13
				<F4[1] > =Data14
				<F4[1] > =Data15
				<F4[1] > =Data16
				<F4[1] > =Data17
				<F4[1] > =Data18
				<F4[1] > =Data19
				<F4[1] > =Data20
				<F4[1] > =Data21
				<F4[1] > =Data22
				<F4[1] > =Data23
				<F4[1] > =Data24
				<F4[1] > =Data25
				<F4[1] > =Data26
				<F4[1] > =Data27
				<F4[1] > =Data28
				<F4[1] > =Data29
				<F4[1] > =Data30
				<F4[1] > =Data31
				<F4[1] > =Data32
				<F4[1] > =Data33
				<F4[1] > =Data34
				<F4[1] > =Data35
			>
		>
	>
>.

S6F11 =s6f11incommon18 INPUT W 
<L [3]
	<U4[1] >=  DataID
	<U4[1] >=  CollEventID
	<L [1]
		<L [2]
			<U4[1] > =ReportId
			<L [31]
				<A[MAX 50] > = TimeStr
				<A[MAX 50] > = PPExecName
				<A[MAX 50] > = Remark
				<U2[1] > =Data0
				<U4[1] > =Data1
				<F4[1] > =Data2
				<F4[1] > =Data3
				<F4[1] > =Data4
				<F4[1] > =Data5
				<F4[1] > =Data6
				<F4[1] > =Data7
				<F4[1] > =Data8
				<F4[1] > =Data9
				<F4[1] > =Data10
				<F4[1] > =Data11
				<F4[1] > =Data12
				<F4[1] > =Data13
				<F4[1] > =Data14
				<F4[1] > =Data15
				<F4[1] > =Data16
				<F4[1] > =Data17
				<F4[1] > =Data18
				<F4[1] > =Data19
				<F4[1] > =Data20
				<F4[1] > =Data21
				<F4[1] > =Data22
				<F4[1] > =Data23
				<F4[1] > =Data24
				<F4[1] > =Data25
                                <F4[1] > =Data26
				<F4[1] > =Data27
			>
		>
	>
>.

S6F11 =s6f11incommon19 INPUT W 
<L [3]
	<U4[1] >=  DataID
	<U4[1] >=  CollEventID
	<L [1]
		<L [2]
			<U4[1] > =ReportId
			<L [43]
				<A[MAX 50] > = TimeStr
				<A[MAX 50] > = PPExecName
				<A[MAX 50] > = Remark
				<U2[1] > =Data0
				<U4[1] > =Data1
				<F4[1] > =Data2
				<F4[1] > =Data3
				<F4[1] > =Data4
				<F4[1] > =Data5
				<F4[1] > =Data6
				<F4[1] > =Data7
				<F4[1] > =Data8
				<F4[1] > =Data9
				<F4[1] > =Data10
				<F4[1] > =Data11
				<F4[1] > =Data12
				<F4[1] > =Data13
				<F4[1] > =Data14
				<F4[1] > =Data15
				<F4[1] > =Data16
				<F4[1] > =Data17
				<F4[1] > =Data18
				<F4[1] > =Data19
				<F4[1] > =Data20
				<F4[1] > =Data21
				<F4[1] > =Data22
				<F4[1] > =Data23
				<F4[1] > =Data24
				<F4[1] > =Data25
                                <F4[1] > =Data26
				<F4[1] > =Data27
                                <F4[1] > =Data28
				<F4[1] > =Data29
				<F4[1] > =Data30
				<F4[1] > =Data31
				<F4[1] > =Data32
				<F4[1] > =Data33
				<F4[1] > =Data34
				<F4[1] > =Data35
				<F4[1] > =Data36
				<F4[1] > =Data37
                                <F4[1] > =Data38
				<F4[1] > =Data39
			>
		>
	>
>.

S6F11 =s6f11incommon20 INPUT W 
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1]> = ReportId
			<L [6]
				<A[MAX 50] > = Date
				<F4[600] > = Data0
				<F4[600] > = Data1
				<F4[600] > = Data2
				<F4[600] > = Data3
				<F4[600] > = Data4
			>
		>
	>
>. 

S6F11 = s6f11incommon25  INPUT W
<L [3]
	<U4[1] >   =DataID
	<U4[1] >   =CollEventID
	<L [2]
		<L [2]
			<U4[1] >    =ReportId0
			<L [1]
				<A[16] >=clock
			>
		>
		<L [2]
			<U4[1] 7>=reportId1
			<L [2]
				<V>=DATA
				<U4[1] >=data5
			>
		>
	>
>. 
S6F11 = s6f11incommon26  INPUT W
<L [3]
	<U4[1] >  =  DataID
	<U4[1] >  =  CollEventID
	<L [1]
		<L [2]
			<U4[1] >  =  ReportId
			<L [3]
				<A[MAX 50] >  =  Date
				<A[MAX 50] >  =  RecipeName
				<U1[1] >  =  Data
			>
		>
	>
>. 
S6F11 = s6f11incommon27  INPUT W
<L [3]
	<U4[1] >    = DataID
	<U4[1] >     = CollEventID
	<L [2]
		<L [2]
			<U4[1] >  =DATA1
			<L [1]
				<A[MAX 100] > = TimeStr
			>
		>
		<L [2]
			<U4[1] >   =DATA2
			<L [2]
				<L [1]
					<U4[1] >  =DATA3
				>
				<U4[1] > =DATA4
			>
		>
	>
>. 
S6F11 = s6f11incommon28  INPUT W
<L [3]
	<U4[1] >= DataID
	<U4[1] >= CollEventID
	<L [1]
		<L [2]
			<U4[1] >=ReportId
			<L [4]
				<A[MAX 100] >=DATA2
				<A[MAX 100] >=DATA3
				<A[MAX 100]>=DATA4
				<U4[1] >=DATA5
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
S2F13 = s2f13pecific OUTPUT W
<V>=DATA.
****************************************************************************
* S2F14    Equipment Constant Data                          H<- E      *
****************************************************************************

S2F14 =s2f14in INPUT 
< L [0]>=RESULT.

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

S2F30 =s2f30onein INPUT 
    <V> = RESULT.

****************************************************************************
* S2F33    Define Report (DR)                                  H -> E      *
****************************************************************************

S2F33 OUTPUT = s2f33out W
<L [2]
    <U4 [1]>=DataID
    <L[1]
        <L [2]
            <U4 [1]>              =ReportID
            <L[1]
               <U4 [1]>           =VariableID            
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
               <U4 [1]>         = EquipStatusID 
               <U4 [1]>         = PPExecNameID                  
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
    <B [1]> = ALCD
    <U4 [1]> = ALID
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
<L [1]
    <B[1] > = ALED

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
    <B [MAX 20000]>                     =Processprogram
>.
S7F3 INPUT =s7f3in W
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <A [MAX 20000] >                   =Processprogram
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
 <V> = EPPD.

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
        <v> = ERRW7
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
            <A 'PP-ID'> 
            <A [MAX 50]> = PPID
        >    
    >
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